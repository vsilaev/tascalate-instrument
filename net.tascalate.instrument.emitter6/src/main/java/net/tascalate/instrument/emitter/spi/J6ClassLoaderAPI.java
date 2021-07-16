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

import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;

class J6ClassLoaderAPI extends ClassLoaderAPI {

    @Override
    Object getClassLoadingLock(ClassLoader classLoader, String className) throws Exception {
        return classLoader;
    }
    
    @Override
    Package getPackage(ClassLoader classLoader, String packageName) throws Exception {
        return (Package)GET_PACKAGE.invoke(classLoader, packageName);
    }

    @Override
    Package definePackage(ClassLoader classLoader, String packageName) throws Exception {
        return (Package)DEFINE_PACKAGE.invoke(classLoader, packageName,
                                              null, null, null, null, null, null, 
                                              null);
    }

    @Override
    Class<?> findLoadedClass(ClassLoader classLoader, String className) throws Exception {
        return (Class<?>)FIND_LOADED_CLASS.invoke(classLoader, className);
    }

    @Override
    Class<?> defineClass(ClassLoader classLoader, String className, byte[] classBytes, ProtectionDomain protectionDomain)  throws Exception {
        return (Class<?>) DEFINE_CLASS.invoke(classLoader, className, classBytes, 0, classBytes.length, protectionDomain);
    }
    
    @Override
    int version() {
        return 6;
    }
    
    private static final Method GET_PACKAGE;    
    private static final Method DEFINE_PACKAGE;
    private static final Method FIND_LOADED_CLASS;
    private static final Method DEFINE_CLASS;
    
    static {
        Class<?> clazz = ClassLoader.class;
        
        GET_PACKAGE       = getMethod(false, clazz, "getPackage", 
                                      String.class);
        DEFINE_PACKAGE    = getMethod(false, clazz, "definePackage", 
                                      String.class, 
                                      String.class, String.class, String.class, String.class, String.class, String.class,
                                      URL.class);
        FIND_LOADED_CLASS = getMethod(false, clazz, "findLoadedClass", 
                                      String.class);
        DEFINE_CLASS      = getMethod(false, clazz,  "defineClass",
                                      String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
    }
}
