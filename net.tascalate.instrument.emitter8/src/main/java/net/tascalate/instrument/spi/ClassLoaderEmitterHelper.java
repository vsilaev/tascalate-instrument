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
package net.tascalate.instrument.spi;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

abstract class ClassLoaderEmitterHelper implements ClassEmitter {
    private final Reference<ClassLoader> classLoaderRef;
    private final AccessControlContext accessCtx;
    
    ClassLoaderEmitterHelper(ClassLoader classLoader) {
        classLoaderRef = new WeakReference<ClassLoader>(classLoader);
        accessCtx = AccessController.getContext();
    }
    
    abstract Class<?> defineClass(String className, 
                                  byte[] classBytes, 
                                  ClassLoader classLoader,
                                  ProtectionDomain protectionDomain) throws Exception;
    
    @Override
    public final Class<?> defineClass(final byte[] classBytes, 
                                      final ProtectionDomain protectionDomain) throws ClassEmitterException {
        final ClassLoader classLoader = classLoaderRef.get();
        if (null == classLoader) {
            throw new IllegalStateException("ClassLoader is unloaded");
        }
        
        final String className;
        
        try {
            className = ReflectionHelper.getClassName(classBytes);
        } catch (IOException ex) {
            throw new ClassEmitterException(ex);
        }

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                
                @Override
                public Class<?> run() throws Exception {
                    return defineClass(className, classBytes, classLoader, protectionDomain);
                }

            }, accessCtx);
        } catch (PrivilegedActionException ex) {
            if (null != ex.getCause()) {
                throw wrap(ex.getCause());
            } else {
                throw new ClassEmitterException(ex);
            }
        }
    }
    
    String describe() {
        ClassLoader classLoader = classLoaderRef.get();
        return "class-loader=" + (null == classLoader ? "<evicted>" : classLoader.toString());
    }
    
    private static ClassEmitterException wrap(Throwable ex) {
        if (ex instanceof ClassEmitterException) {
            return (ClassEmitterException)ex;
        } else {
            return new ClassEmitterException(ex);
        }
    }
}
