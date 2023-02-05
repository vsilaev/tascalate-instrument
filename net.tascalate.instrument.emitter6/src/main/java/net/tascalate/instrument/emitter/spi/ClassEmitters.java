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

import net.tascalate.instrument.emitter.spi.ClassEmitters;

public final class ClassEmitters {
    
    private ClassEmitters() {}

    public static String classNameOf(byte[] classBytes) {
        return ReflectionHelper.getClassName(classBytes);
    }

    public static ClassEmitter of(ClassLoader classLoader) {
        return of(classLoader, true);
    }
    
    public static ClassEmitter of(ClassLoader classLoader, boolean mandatory) {
        if (null == classLoader) {
            throw new IllegalArgumentException("\"classLoader\" may not be null");
        }
        if (classLoader instanceof ClassEmitter) {
            return (ClassEmitter)classLoader;
        }
        return new ClassLoaderEmitter(classLoader);
    }
    
    public static ClassEmitter of(Object moduleOrClass) {
        return of(moduleOrClass, true);
    }

    public static ClassEmitter of(Object moduleOrClass, boolean mandatory) {
        if (null == moduleOrClass) {
            throw new IllegalArgumentException("\"moduleOrClass\" may not be null");
        }
        return of(moduleOrClass, null, mandatory);
    }

    /*
    public static ClassEmitter of(Object moduleOrClass, ClassLoader classLoader) {
        return of(moduleOrClass, classLoader, true);
    }
    */
    
    static ClassEmitter of(Object moduleOrClass, ClassLoader classLoader, boolean mandatory) {
        ClassLoader baseClassLoader = null;
        if (null != moduleOrClass) {
            if (Class.class == moduleOrClass.getClass()) {
                // Ok, it's class
                Class<?> clazz = (Class<?>)moduleOrClass;
                baseClassLoader = clazz.getClassLoader();
            } else {
                String moduleClassName = "java.lang.Module"; 
                // It's a final class, so name check is ok
                if (!moduleClassName.equals(moduleOrClass)) {
                    throw new IllegalArgumentException("First parameter should be either null" + 
                                                       " or instance of " + Class.class.getName() + 
                                                       " or instance of " + moduleClassName);
                } else {
                    throw new IllegalStateException("Unexpected usage of Java 8 version of library inside Java 9+ environment");
                }
            }
            // either class or module
        }
        return ClassEmitters.of(ReflectionHelper.getBestClassLoader(baseClassLoader, classLoader), mandatory);
    }
}
