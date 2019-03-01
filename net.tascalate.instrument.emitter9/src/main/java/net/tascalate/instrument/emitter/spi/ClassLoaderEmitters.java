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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.security.ProtectionDomain;

import net.tascalate.instrument.emitter.spi.ClassEmitter;
import net.tascalate.instrument.emitter.spi.ClassEmitters;

class ClassLoaderEmitters implements ClassEmitters.Factory {

    private final ClassLoaderEmitterHelper emitter;

    ClassLoaderEmitters(ClassLoader classLoader) {
        emitter = new ClassLoaderEmitterHelper(classLoader) {
            @Override
            Class<?> defineClass(String className, 
                                 byte[] classBytes, 
                                 ClassLoader classLoader,
                                 ProtectionDomain protectionDomain) throws Exception {

                return ClassLoaderEmitters.defineClass(className, classBytes, classLoader, protectionDomain);
            }
        };
    }

    @Override
    public ClassEmitter create(String packageName)  {
        return emitter;
    }

    static Class<?> defineClass(String className, 
                                byte[] classBytes, 
                                ClassLoader classLoader,
                                ProtectionDomain protectionDomain) throws Exception {
        
        try {
            Object lock = (Object)GET_CLASS_LOADING_LOCK.invokeExact(classLoader, className);
            Class<?> clazz;
            synchronized (lock) {
                clazz = (Class<?>)FIND_LOADED_CLASS.invokeExact(classLoader, className);
                if (null == clazz) {
                    String packageName = ReflectionHelper.packageNameOf(className);
                    if (null != packageName) {
                        Package p = classLoader.getDefinedPackage(packageName);
                        if (null == p) {
                            p = (Package)DEFINE_PACKAGE.invoke(classLoader, packageName,
                                                               (String)null, (String)null, (String)null, 
                                                               (String)null, (String)null, (String)null, 
                                                               (URL)null);
                        }
                    }
                    clazz = (Class<?>)DEFINE_CLASS.invokeExact(
                       classLoader, className, classBytes, 0, classBytes.length, protectionDomain
                    );
                } else {
                    // throw ex???
                }
            }
            return clazz;            
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ClassEmitterException(ex);
        }
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[method=reflection, supported-packages=<any>, " + emitter.describe() + "]"; 
    }
    
    private static final MethodHandle FIND_LOADED_CLASS;
    private static final MethodHandle DEFINE_CLASS;
    private static final MethodHandle GET_CLASS_LOADING_LOCK; 
    private static final MethodHandle DEFINE_PACKAGE;

    private static MethodHandle getMethod(MethodHandles.Lookup lookup, 
                                          Class<?> clazz,
                                          String methodName, 
                                          Class<?> returnType, 
                                          Class<?>... argTypes) throws ReflectiveOperationException {
        return lookup.findVirtual(clazz, methodName, MethodType.methodType(returnType, argTypes));
    }
    
    static {
        Class<?> clazz = ClassLoader.class;
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            FIND_LOADED_CLASS = getMethod(lookup, clazz, "findLoadedClass", Class.class, String.class);
            
            DEFINE_CLASS = getMethod(lookup, clazz,  "defineClass", Class.class, 
                String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
            
            GET_CLASS_LOADING_LOCK = getMethod(lookup, clazz, "getClassLoadingLock", Object.class, String.class);
            
            DEFINE_PACKAGE = getMethod(lookup, clazz, "definePackage", Package.class, 
                String.class, 
                String.class, String.class, String.class, String.class, String.class, String.class,
                URL.class);
            
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

}
