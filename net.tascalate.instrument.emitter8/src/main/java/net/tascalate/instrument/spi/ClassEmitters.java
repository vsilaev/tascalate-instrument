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

import java.util.Objects;

import net.tascalate.instrument.spi.ClassEmitter;
import net.tascalate.instrument.spi.ClassEmitters;

public final class ClassEmitters {
    
    private ClassEmitters() {}
    
    public static interface Factory {
        /**
         * Lookup for a {@link ClassEmitter} for the given package.
         * 
         * @param packageName the name of the package (com.company.subpackage) where
         *                    classes will be defined
         * @return the {@link ClassEmitter} used to dynamically create classes inside 
         *         the given package, or empty if the package is not "open"
         * @throws ReflectiveOperationException
         */
        abstract ClassEmitter create(String packageName) throws ClassEmitterException;
    }

    public static Factory of(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader);
        if (classLoader instanceof Factory) {
            return (Factory)classLoader;
        }
        return new ClassLoaderEmitters(classLoader);
    }

    public static Factory of(Object moduleOrClass, ClassLoader classLoader) {
        ClassLoader altClassLoader = null;
        if (null != moduleOrClass) {
            if (moduleOrClass.getClass() == Class.class) {
                // Ok, it's class
                Class<?> clazz = (Class<?>)moduleOrClass;
                altClassLoader = clazz.getClassLoader();
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
        return ClassEmitters.of(ReflectionHelper.getBestClassLoader(altClassLoader, classLoader));
    }
}
