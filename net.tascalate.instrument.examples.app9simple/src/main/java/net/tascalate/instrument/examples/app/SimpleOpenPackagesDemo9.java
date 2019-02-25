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
package net.tascalate.instrument.examples.app;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

public class SimpleOpenPackagesDemo9 {

    public static void main(String[] args) throws Throwable {
    	MyClassLoader myCL = new MyClassLoader(ClassLoader.getSystemClassLoader());
    	System.out.println(myCL + " ? " + myCL.isRegisteredAsParallelCapable());
    	System.out.println(myCL.safeGetClassLoadingLock("a.x.y.ZClass"));
        System.out.println("Hello");

        Module myModule = SimpleOpenPackagesDemo9.class.getModule();
        System.out.println("App is running in module " + myModule.getName());

        demoModule("net.tascalate.instrument.examples.app.dynamic.Demo");
    }

    static void demoModule(String dynamicClassName) throws ReflectiveOperationException, IOException {
        
        System.out.println(">>>>>>>>");
        Module otherModule = SimpleOpenPackagesDemo9.class.getModule();
        System.out.println("Instrumenting module " + otherModule);

        @SuppressWarnings("unchecked")
        Class<? extends Runnable> cls = (Class<? extends Runnable>) 
        defineClassDynamically(otherModule,
                               dynamicClassName,
                               readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
                               SimpleOpenPackagesDemo9.class.getProtectionDomain());

        // Pretty questionable deprecation of API
        @SuppressWarnings("deprecation")
        Runnable obj = cls.newInstance();
        obj.run();
        System.out.println("<<<<<<<<");
    }

    private static Class<?> defineClassDynamically(Module module, 
                                                   String className, 
                                                   byte[] classBytes,
                                                   ProtectionDomain pd) throws ReflectiveOperationException {

        ClassDefiners.Lookup definers = ClassDefiners.of(module, module.getClassLoader());
        /*
         * Effectively, the call above is just the same as ClassDefiners.of(module); Two
         * args form is used to show that same pattern will be used with Java 6-8 and Java
         * 9-11+ from instrumentation agents. Obviously, Java 6-8 agent will use
         * ClassDefiners.of(null, classLoaderArg);
         */

        System.out.println("Get Definers: " + definers);

        String packageName = className.substring(0, className.lastIndexOf('.'));

        ClassDefiner definer = definers.lookup(packageName);
        if (null == definer) {
            throw new IllegalArgumentException("No class definer exists for package " + packageName);
        }

        System.out.println("Created definer: " + definer);

        return definer.defineClass(classBytes, pd);
    }

    private static byte[] readResource(String name) throws IOException {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(name).readAllBytes();
    }

    static class MyClassLoader extends ClassLoader {
    	public MyClassLoader(ClassLoader parent) {
    		super(parent);
		}
    	
        public Object safeGetClassLoadingLock(String name) {
            if (null == GET_CLASS_LOADING_LOCK) {
                return this;
            }
            try {
                return GET_CLASS_LOADING_LOCK.invoke(this, name);
            } catch (IllegalAccessException ex) {
                // Should not happen
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                // Should not happen
                throw ex;
            } catch (InvocationTargetException ex) {
                // Should not happen
                throw new RuntimeException(ex);
            }
        }
    	
        private static Method getClassLoaderMethodOrNull(String name, Class<?>... args) {
            try {
                Method m = ClassLoader.class.getDeclaredMethod(name, args);
                try { m.setAccessible(true); } catch (Exception ex) {}
                return m;
            } catch (NoSuchMethodException ex) {
                // OK, JDK version is less then 1.7
                return null;
            } catch (SecurityException ex) {
                // Should be available, if method exists
                throw ex;
            }
        }
        
        private static final Method GET_CLASS_LOADING_LOCK = 
            getClassLoaderMethodOrNull("getClassLoadingLock", String.class);
        
        static { 
            Method registerAsParallelCapable = getClassLoaderMethodOrNull("registerAsParallelCapable");
            if (null != registerAsParallelCapable) {
                try {
                	registerAsParallelCapable.invoke(null);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalArgumentException ex) {
                    throw ex;
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }
}
