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
package net.tascalate.instrument.emitter;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;

import net.tascalate.instrument.emitter.ClassEmitter;
import net.tascalate.instrument.emitter.ClassEmitters;

class ClassLoaderEmitters implements ClassEmitters.Factory {

    private final WeakReference<ClassLoader> classLoaderRef;
    
    private final ClassEmitter emitter = new ClassEmitter() {
        @Override
        public Class<?> defineClass(byte[] classBytes, ProtectionDomain protectionDomain) throws ClassEmitterException {
            ClassLoader classLoader = classLoaderRef.get();
            if (null == classLoader) {
                throw new IllegalStateException("ClassLoader is unloaded");
            }
            try {
                String className = ReflectionHelper.getClassName(classBytes);
                Object lock = null == GET_CLASS_LOADING_LOCK ? classLoader : GET_CLASS_LOADING_LOCK.invoke(classLoader, className);
                Class<?> clazz;
                synchronized (lock) {
                    clazz = (Class<?>)FIND_LOADED_CLASS.invoke(classLoader, className);
                    if (null == clazz) {
                        int lastDot = className.lastIndexOf('.');
                        String packageName = lastDot > 0 ? className.substring(0, lastDot) : null;
                        if (null != packageName) {
                            Object p = GET_PACKAGE.invoke(classLoader, packageName);
                            if (null == p) {
                                DEFINE_PACKAGE.invoke(classLoader, packageName,
                                                      null, null, null, null, null, null, 
                                                      null);
                            }
                        }
                        clazz = (Class<?>) DEFINE_CLASS.invoke(classLoader, (String) null, classBytes, 0, classBytes.length, protectionDomain);
                    } else {
                        // throw ex???
                    }
                }
                return clazz;
            } catch (Error ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new ClassEmitterException(ex);
            }
        }
    };

    ClassLoaderEmitters(ClassLoader classLoader) {
        this.classLoaderRef = new WeakReference<ClassLoader>(classLoader);
    }

    @Override
    public ClassEmitter create(String packageName) {
        return emitter;
    }
    
    private static Method getMethod(boolean optional, Class<?> clazz, String methodName, Class<?>... args) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, args);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            // OK, some JDK versions might miss methods
            if (optional) {
                return null;
            } else {
                throw new RuntimeException(ex);
            }
        } catch (SecurityException ex) {
            // Should be available, if method exists
            throw ex;
        }
    }

    private static final Method FIND_LOADED_CLASS;
    private static final Method DEFINE_CLASS;
    private static final Method GET_CLASS_LOADING_LOCK; 
    private static final Method GET_PACKAGE;    
    private static final Method DEFINE_PACKAGE;
    
    static {
        Class<?> clazz = ClassLoader.class;
        
        FIND_LOADED_CLASS = getMethod(false, clazz, "findLoadedClass", String.class);
        
        DEFINE_CLASS = getMethod(false, clazz,  "defineClass", 
            String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
        
        GET_CLASS_LOADING_LOCK = getMethod(true, clazz, "getClassLoadingLock", String.class);
        
        GET_PACKAGE = getMethod(false, clazz, "getPackage", String.class);
        
        DEFINE_PACKAGE = getMethod(false, clazz, "definePackage", 
            String.class, 
            String.class, String.class, String.class, String.class, String.class, String.class,
            URL.class);
    }
}
