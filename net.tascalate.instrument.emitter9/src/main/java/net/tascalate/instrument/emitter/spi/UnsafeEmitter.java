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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

import sun.misc.Unsafe;

class UnsafeEmitter implements ClassEmitter {
    private final Reference<ClassLoader> classLoaderRef;
    private final Unsafe unsafe;
    private final AccessControlContext accessCtx;
    
    UnsafeEmitter(ClassLoader classLoader) {
        this(classLoader, UNSAFE);
    }
    
    private UnsafeEmitter(ClassLoader classLoader, Unsafe unsafe_) {
        classLoaderRef = new WeakReference<ClassLoader>(classLoader);
        unsafe = unsafe_;
        accessCtx = AccessController.getContext();
    }
    
    @SuppressWarnings("removal")
    private Class<?> defineClass(String className, 
                                 byte[] classBytes, 
                                 ClassLoader classLoader,
                                 ProtectionDomain protectionDomain) throws Exception {
        return unsafe.defineClass(className, classBytes, 0, classBytes.length, classLoader, protectionDomain);
    }
    
    @Override
    public final Class<?> defineClass(final byte[] classBytes, 
                                      final ProtectionDomain protectionDomain) throws ClassEmitterException {
        final ClassLoader classLoader = classLoaderRef.get();
        if (null == classLoader) {
            throw new IllegalStateException("ClassLoader is unloaded");
        }
        
        final String className = ReflectionHelper.getClassName(classBytes);

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
    
    @Override
    public String toString() {
        return getClass().getName() + "[method=unsafe, supported-packages=<any>, " + describe() + "]"; 
    }
    
    private String describe() {
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
    
    private static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); // Internal reference
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}
