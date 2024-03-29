/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2019-2022, Valery Silaev
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

class ModuleClassEmitter implements ClassEmitter {

    private final WeakReference<Module> targetModule;
    private final List<WeakReference<Class<?>>> packageClasses;
    private final OpenPackageAction openPackage;
    private final MethodHandles.Lookup selfLookup = MethodHandles.lookup();
    private final Map<String, WeakReference<MethodHandles.Lookup>> cachedLookupRefs = new HashMap<>();

    ModuleClassEmitter(Module targetModule, Class<?>[] packageClasses, OpenPackageAction openPackage) {
        this.targetModule = new WeakReference<>(targetModule);
        this.packageClasses = Stream.of(packageClasses)
                                    .map(ModuleClassEmitter::weakReferenceOf)
                                    .collect(Collectors.toList());
        this.openPackage = openPackage;
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
        MethodHandles.Lookup lookup;
        synchronized (cachedLookupRefs) {
            WeakReference<MethodHandles.Lookup> lookupRef = cachedLookupRefs.get(packageName);
            lookup = lookupRef == null ? null : lookupRef.get();
            if (null == lookup) {
                lookup = lookupInternal(packageName);
                if (null != lookup) {
                    cachedLookupRefs.put(packageName, new WeakReference<>(lookup));
                }
            }
        }
        return toClassEmitter(lookup);
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

    private MethodHandles.Lookup lookupInternal(String packageName) throws ClassEmitterException {
        Module target = targetModule.get();
        Module self   = getClass().getModule();
        if (null == target) {
            throw new IllegalStateException("Module is unloaded");
        }
        for (WeakReference<Class<?>> packageClassRef : packageClasses) {
            Class<?> packageClass = packageClassRef.get();
            if (null == packageClass) {
                // May be unloaded
                continue;
            }
            if (packageClass.getPackageName().equals(packageName)) {
                // Sanity check first to help app developer follow our rules
                if (target.isOpen(packageName, self) || openPackage.run(packageName, target, self)) {
                    // Allows concrete implementations of AbstractOpenPackage
                    // to have [package-]private constructor
                    try {
                        return MethodHandles.privateLookupIn(packageClass, selfLookup);
                    } catch (IllegalAccessException e) {
                        throw new ClassEmitterException(e);
                    }
                } else {
                    throw new IllegalStateException(
                        "Module " + target.getName() + " shows an intent to support instrumentation " +
                        "(annotation @" + AllowDynamicClasses.class.getSimpleName() + " is present " +
                        "with " + AbstractOpenPackage.class.getSimpleName() + " = " + packageClass.getName() + "), " +
                        "however package " + packageName + " is not open for module " + self.getName()
                    );
                }
            }
        }
        return null;
    }
    
    private static ClassEmitter toClassEmitter(MethodHandles.Lookup lookup) {
        if (null == lookup) {
            return null;
        } else {
            return (bytes, domain) -> {
                try {
                    return lookup.defineClass(bytes);
                } catch (IllegalAccessException e) {
                    throw new ClassEmitterException(e);
                }
            }; 
        }
    }
    
    private static <T> WeakReference<Class<?>> weakReferenceOf(Class<T> cls) {
        Class<?> casted = cls;
        return new WeakReference<>(casted);
    }
}
