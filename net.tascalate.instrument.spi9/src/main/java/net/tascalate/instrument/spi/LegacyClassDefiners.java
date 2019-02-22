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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package net.tascalate.instrument.spi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Optional;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

class LegacyClassDefiners implements ClassDefiners {

    private final WeakReference<ClassLoader> classLoaderRef;
    
    private final ClassDefiner definer = new ClassDefiner() {
        @Override
        public Class<?> defineClass(byte[] classBytes, ProtectionDomain protectionDomain) throws ReflectiveOperationException {
            ClassLoader classLoader = classLoaderRef.get();
            if (null == classLoader) {
                throw new IllegalStateException("ClassLoader is unloaded");
            }
            try {
                Class<?> clazz = (Class<?>) DEFINE_CLASS.invokeExact(
                    classLoader, (String) null, classBytes, 0, classBytes.length, protectionDomain
                );
                return clazz;
            } catch (Error | RuntimeException | ReflectiveOperationException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new ReflectiveOperationException(ex);
            }
        }
    };

    LegacyClassDefiners(ClassLoader classLoader) {
        this.classLoaderRef = new WeakReference<>(classLoader);
    }

    @Override
    public Optional<ClassDefiner> lookup(String packageName) throws ReflectiveOperationException {
        return Optional.of(definer);
    }

    private static final MethodHandle DEFINE_CLASS;

    // With Java 9 it's necessary to do a private lookup
    // to the package opened via command line
    private static MethodHandle findDefineClassJ9() throws ReflectiveOperationException {
        return MethodHandles.privateLookupIn(ClassLoader.class, MethodHandles.lookup())
                            .findVirtual(ClassLoader.class,
                                         "defineClass", 
                                         MethodType.methodType(Class.class, 
                                                               String.class, 
                                                               byte[].class, 
                                                               int.class, 
                                                               int.class,
                                                               ProtectionDomain.class));
    }

    static {
        try {
            DEFINE_CLASS = findDefineClassJ9();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

}
