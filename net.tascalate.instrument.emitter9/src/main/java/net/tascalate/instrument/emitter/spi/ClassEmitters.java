/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Valery Silaev
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.tascalate.instrument.emitter.spi;

import java.util.Objects;

import net.tascalate.instrument.emitter.api.AbstractOpenPackage;
import net.tascalate.instrument.emitter.api.AllowDynamicClasses;
import net.tascalate.instrument.emitter.spi.ClassEmitter;
import net.tascalate.instrument.emitter.spi.ClassEmitters;

public final class ClassEmitters {
    
    private ClassEmitters() {}

    public static String classNameOf(byte[] classBytes) {
        return ReflectionHelper.getClassName(classBytes);
    }
    
    public static String packageNameOf(byte[] classBytes) {
        return ReflectionHelper.packageNameOf(classNameOf(classBytes));
    }
    
    public static ClassEmitter of(ClassLoader classLoader) {
        return of(classLoader, true);
    }

    public static ClassEmitter of(ClassLoader classLoader, boolean mandatory) {
        Objects.requireNonNull(classLoader);
        if (classLoader instanceof ClassEmitter) {
            // Custom class loader that defines method explicitly
            return (ClassEmitter)classLoader;
        } else if (CLASS_LOADER_EMITTERS_SUPPORTED) {
            // Good-old Java 8 class loader reflection
            // if Java 9 is started with option
            // --add-opens java.base/java.lang=net.tascalate.instrument.emitter
            // or to all unnamed modules and we are shaded inside unnamed module
            return new ClassLoaderEmitter(classLoader);
        } else if (UnsafeSupport.isSupported()) {
            // Short flirt with Java 9 / 10 
            return new UnsafeEmitter(classLoader);
        } else {
            // Out of luck on this path
            if (mandatory) {
                throw new IllegalStateException(
                    "ClassLoader-based injection is unavailable.\n" +
                    "Please either start Java with command-line option --add-opens java.base/java.lang=" +
                    (SELF_MODULE.isNamed() ? SELF_MODULE.getName() : "ALL-UNNAMED") + "\n" +
                    "or configure your module with @" + AllowDynamicClasses.class.getName() + " annotation and \n" +
                    "use " + ClassEmitters.class.getName() + ".of(moduleOrClass, classLoader) method."
                );
            } else {
                return null;
            }
        }
    }

    public static ClassEmitter of(Module module) {
        return of(module, true);
    }

    public static ClassEmitter of(Module module, boolean mandatory) {
        Objects.requireNonNull(module);
        AllowDynamicClasses settings = module.getAnnotation(AllowDynamicClasses.class);
        if (null == settings) {
            if (mandatory) {
                throw new IllegalStateException(
                    "Module " + module.getName() + " does not allow defying dynamic classes " + 
                    "(not annotated with " + AllowDynamicClasses.class.getName() + ")"
                );
            } else {
                return null;
            }
        }
        return of(module, settings, null, OpenPackageAction.NOP);
    }
    
    public static ClassEmitter of(Object moduleOrClass, ClassLoader classLoader) {
        return of(moduleOrClass, classLoader, true);
    }
    
    public static ClassEmitter of(Object moduleOrClass, ClassLoader classLoader, boolean mandatory) {
        return of(moduleOrClass, classLoader, mandatory, OpenPackageAction.NOP);
    }
    
    static ClassEmitter of(Object moduleOrClass, 
                           ClassLoader classLoader, 
                           boolean mandatory,
                           OpenPackageAction openPackage) {
        if (moduleOrClass instanceof Class) {
            Class<?> clazz = (Class<?>)moduleOrClass;
            ClassLoader altClassLoader = clazz.getClassLoader();
            if (mayUseClassLoaderEmitters(classLoader) || (altClassLoader instanceof ClassEmitter)) {
                return ClassEmitters.of(ReflectionHelper.getBestClassLoader(altClassLoader, classLoader), mandatory);
            } else {
                Module module = clazz.getModule();
                AllowDynamicClasses settings = module.getAnnotation(AllowDynamicClasses.class);
                if (settings != null) {
                    // Module is configured
                    return of(module, settings, clazz, openPackage);
                } else {
                    // Non-configured module
                    // The best we can do is a single-package ModuleClassEmitters
                    String packageName = clazz.getPackageName();
                    if (module.isOpen(packageName, SELF_MODULE) || openPackage.run(packageName, module, SELF_MODULE)) {
                        if (!SELF_MODULE.canRead(module)) {
                            // Obviously, we unaware initially of all modules that requires
                            // dynamic class definitions.
                            // So it's necessary to add "read" access dynamically
                            SELF_MODULE.addReads(module);
                        }
                        return new ModuleClassEmitter(module, new Class[] {clazz}, OpenPackageAction.NOP /* already opened */);
                    } else {
                        // In fact this will throw exception that explains everything
                        return of(classLoader, mandatory);
                    }
                }
            }
        } else if (moduleOrClass instanceof Module) {
            Module module = (Module)moduleOrClass;
            AllowDynamicClasses settings = module.getAnnotation(AllowDynamicClasses.class);
            if (settings != null) {
                // Module is configured
                // TODO what if classLoader supplied is Factory?
                // what scenario should win?
                return of(module, settings, null, openPackage);
            } else {
                // Non-configured module
                // try our best with class loaders
                return of(ReflectionHelper.getBestClassLoader(module.getClassLoader(), classLoader), mandatory);
            }
        } else if (null != moduleOrClass) {
            throw new IllegalArgumentException(
                "First parameter should be either null" + 
                " or instance of " + Class.class.getName() + 
                " or instance of " + Module.class.getName()
            );
        } else {
            return ClassEmitters.of(classLoader, mandatory);
        }
    }
    
    private static ClassEmitter of(Module module, 
                                   AllowDynamicClasses settings, 
                                   Class<?> extraClass,
                                   OpenPackageAction openPackage) {
        Class<? extends AbstractOpenPackage>[] settingsClasses = settings.value();
        Class<?>[] packageClasses;
        if (extraClass == null) {
            packageClasses = settingsClasses;
        } else {
            int originalSize = settingsClasses.length;
            packageClasses = new Class[originalSize + 1];
            System.arraycopy(settingsClasses, 0, packageClasses, 0, originalSize);
            packageClasses[originalSize] = extraClass;
            
        }
        if (packageClasses.length == 0) {
            // TODO: empty vs error?
            // return EmptyClassEmitters.INSTANCE;
            throw new IllegalStateException("There are no packages opened to emitter by the module " + module.getName());
        }

        if (!SELF_MODULE.canRead(module)) {
            // Obviously, we unaware initially of all modules that requires
            // dynamic class definitions.
            // So it's necessary to add "read" access dynamically
            SELF_MODULE.addReads(module);
        }

        return new ModuleClassEmitter(module, packageClasses, openPackage);        
    }
    
    private static boolean mayUseClassLoaderEmitters(ClassLoader classLoader) {
        return (classLoader instanceof ClassEmitter) ||
               CLASS_LOADER_EMITTERS_SUPPORTED  ||
               UnsafeSupport.isSupported();
    }
    
    private static Module SELF_MODULE;
    private static boolean CLASS_LOADER_EMITTERS_SUPPORTED;
    static {
        SELF_MODULE = ClassEmitters.class.getModule();
        
        boolean supported = false;
        Module javaModule = ClassLoader.class.getModule();
        if (SELF_MODULE.canRead(javaModule)) {
            // Sure, it can, but anyway... ))
            // The next check is crucial -- is java.lang package opened
            // either via command-line argument or when our code
            // is shaded inside unnamed module
            supported = javaModule.isOpen(ClassLoader.class.getPackageName(), SELF_MODULE);
        }
        CLASS_LOADER_EMITTERS_SUPPORTED = supported;
    }
}
