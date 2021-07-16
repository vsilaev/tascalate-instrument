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
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;

class J7ClassLoaderAPI extends ClassLoaderAPI {
    Object getClassLoadingLock(ClassLoader classLoader, String className) throws Throwable {
        return (Object)GET_CLASS_LOADING_LOCK.invokeExact(classLoader, className);
    }
    
    Package getPackage(ClassLoader classLoader, String packageName) throws Throwable {
        return (Package)GET_PACKAGE.invokeExact(classLoader, packageName);
    }
    
    Package definePackage(ClassLoader classLoader, String packageName) throws Throwable {
        return (Package)DEFINE_PACKAGE.invoke(classLoader, packageName,
                                              (String)null, (String)null, (String)null, 
                                              (String)null, (String)null, (String)null, 
                                              (URL)null);        
    }
    
    Class<?> findLoadedClass(ClassLoader classLoader, String className) throws Throwable {
        return (Class<?>)FIND_LOADED_CLASS.invokeExact(classLoader, className);
    }
    
    Class<?> defineClass(ClassLoader classLoader, String className, byte[] classBytes, ProtectionDomain protectionDomain) throws Throwable {
        return (Class<?>)DEFINE_CLASS.invokeExact(classLoader, className, classBytes, 0, classBytes.length, protectionDomain);
    }
    
    @Override
    int version() {
        return 7;
    }
    
    private static MethodHandle getMethod(MethodHandles.Lookup lookup, 
                                          Class<?> clazz,
                                          String methodName, 
                                          Class<?> returnType, 
                                          Class<?>... args) throws ReflectiveOperationException {
        
        Method method = getMethod(false, clazz, methodName, args);
        return lookup.unreflect(method);
    }

    private static final MethodHandle GET_CLASS_LOADING_LOCK;
    private static final MethodHandle GET_PACKAGE;
    private static final MethodHandle DEFINE_PACKAGE;
    private static final MethodHandle FIND_LOADED_CLASS;
    private static final MethodHandle DEFINE_CLASS;

    
    static {
        Class<?> clazz = ClassLoader.class;
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup().in(clazz);
            GET_CLASS_LOADING_LOCK = getMethod(lookup, clazz, "getClassLoadingLock", 
                                               Object.class, 
                                               String.class);
            GET_PACKAGE            = getMethod(lookup, clazz, "getPackage", 
                                               Package.class, 
                                               String.class);
            FIND_LOADED_CLASS      = getMethod(lookup, clazz, "findLoadedClass", 
                                               Class.class, 
                                               String.class);
            DEFINE_PACKAGE         = getMethod(lookup, clazz, "definePackage", 
                                               Package.class, 
                                               String.class, 
                                               String.class, String.class, String.class, String.class, String.class, String.class,
                                               URL.class);
            DEFINE_CLASS           = getMethod(lookup, clazz,  "defineClass", 
                                               Class.class, 
                                               String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }  
}
