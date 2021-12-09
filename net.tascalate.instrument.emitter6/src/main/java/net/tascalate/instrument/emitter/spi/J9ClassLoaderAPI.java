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

import java.security.ProtectionDomain;

/**
 * Placeholder class -- will be replaced with the code from *.emitter9
 * @author vsilaev
 *
 */
class J9ClassLoaderAPI extends ClassLoaderAPI {

    @Override
    Object getClassLoadingLock(ClassLoader classLoader, String className) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Package getPackage(ClassLoader classLoader, String packageName) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Package definePackage(ClassLoader classLoader, String packageName) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Class<?> findLoadedClass(ClassLoader classLoader, String className) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Class<?> defineClass(ClassLoader classLoader, String className, byte[] classBytes, ProtectionDomain protectionDomain) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    int version() {
        throw new UnsupportedOperationException();
    }
}