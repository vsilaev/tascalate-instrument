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
import java.util.Optional;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

public interface ClassDefiners {

    /**
     * Lookup for a {@link ClassDefiner} for the given package.
     * 
     * @param packageName the name of the package (com.company.subpackage) where
     *                    classes will be defined
     * @return the {@link Optional} containing the {@link ClassDefiner} used to
     *         dynamically create classes inside the given package, or empty if the
     *         package is not "open"
     * @throws ReflectiveOperationException
     */
    abstract Optional<ClassDefiner> lookup(String packageName) throws ReflectiveOperationException;

    public static ClassDefiners of(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader);
        return new LegacyClassDefiners(classLoader);
    }

    public static ClassDefiners of(Object module, ClassLoader classLoader) {
        if (null != module) {
            String moduleClassName = "java.lang.Module"; 
            if (!moduleClassName.equals(module)) {
                throw new IllegalArgumentException("First parameter should be either null or instance of " + moduleClassName);
            }
        }
        return ClassDefiners.of(classLoader);
    }
}
