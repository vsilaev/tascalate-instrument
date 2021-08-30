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

import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.tascalate.instrument.emitter.api.AbstractOpenPackage;
import net.tascalate.instrument.emitter.api.AllowDynamicClasses;
import net.tascalate.instrument.emitter.spi.ClassEmitter;

class ModuleClassEmitter implements ClassEmitter {

    private final WeakReference<Module> targetModule;
    private final List<WeakReference<Class<?>>> packageClasses;
    private final MethodHandles.Lookup selfLookup = MethodHandles.lookup();
    private final Map<String, ClassEmitter> cachedDefiners = new HashMap<>();

    ModuleClassEmitter(Module targetModule, Class<?>[] packageClasses) {
        this.targetModule = new WeakReference<>(targetModule);
        // TODO: is keeping hard references is ok / mandatory?
        this.packageClasses = Stream.of(packageClasses)
                                    .map(ModuleClassEmitter::weakReferenceOf)
                                    .collect(Collectors.toList());
    }
    
    @Override
    public Class<?> defineClass(byte[] classBytes, ProtectionDomain protectionDomain) throws ClassEmitterException {
        String packageName = ReflectionHelper.packageNameOf(ReflectionHelper.getClassName(classBytes));
        ClassEmitter delegate = create(packageName);
        if (null == delegate) {
            throw new ClassEmitterException("No class emitter available for package " + packageName);
        } else {
            return delegate.defineClass(classBytes, protectionDomain);
        }
    }

    private ClassEmitter create(String packageName) throws ClassEmitterException {
        ClassEmitter definer;
        synchronized (cachedDefiners) {
            definer = cachedDefiners.get(packageName);
            if (null == definer) {
                definer = lookupInternal(packageName);
            }
            cachedDefiners.put(packageName, definer);
        }
        return definer == NO_DEFINER ? null : definer;
    }
    
    @Override
    public String toString() {
        String packages =
        packageClasses.stream()
                      .map(Reference::get)
                      .filter(Objects::nonNull)
                      .map(Class::getPackageName)
                      .collect(Collectors.joining(", "));
        Module target = targetModule.get();
        String moduleName = null == target ? "<evicted>" : target.getName();
        return getClass().getName() + "[method=lookup, supported-packages=" + packages + 
                                      ", module="  + moduleName + "]"; 
    }

    private ClassEmitter lookupInternal(String packageName) throws ClassEmitterException {
        Module target = targetModule.get();
        Module self   = getClass().getModule();
        if (null == target) {
            throw new IllegalStateException("Module is unloaded");
        }
        for (WeakReference<Class<?>> packageClassRef : packageClasses) {
            Class<?> packageClass = packageClassRef.get();
            if (null == packageClass) {
                // Or exception?
                continue;
            }
            if (packageClass.getPackageName().equals(packageName)) {
                // Sanity check first to help app developer follow our rules
                if (!target.isOpen(packageName, self)) {
                    throw new IllegalStateException(
                        "Module " + target.getName() + " shows an intent to support instrumentation " +
                        "(annotation @" + AllowDynamicClasses.class.getSimpleName() + " is present " +
                        "with " + AbstractOpenPackage.class.getSimpleName() + " = " + packageClass.getName() + "), " +
                        "however package " + packageName + " is not open for module " + self.getName()
                    );
                }
                // Allows concrete implementations of AbstractOpenPackage
                // to have [package-]private constructor
                try {
                    MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(packageClass, selfLookup);
                    return (bytes, domain) -> {
                        try {
                            return lookup.defineClass(bytes);
                        } catch (IllegalAccessException e) {
                            throw new ClassEmitterException(e);
                        }
                    };
                } catch (IllegalAccessException e) {
                    throw new ClassEmitterException(e);
                }
            }
        }
        return NO_DEFINER;
    }
    
    private static <T> WeakReference<Class<?>> weakReferenceOf(Class<T> cls) {
        Class<?> casted = cls;
        return new WeakReference<>(casted);
    }

    private static final ClassEmitter NO_DEFINER = (bytes, domain) -> null;
}
