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
package net.tascalate.instrument.examples.app8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

import net.tascalate.instrument.emitter.spi.ClassEmitter;
import net.tascalate.instrument.emitter.spi.ClassEmitterException;
import net.tascalate.instrument.emitter.spi.ClassEmitters;

public class OpenPackagesDemo8 {
    
    public static void main(String[] args) throws Throwable {
        System.out.println("Hello");

        System.out.println("App is running");

        demoModule("net.tascalate.instrument.examples.app8.surprise.Demo");
    }

    static void demoModule(String dynamicClassName) throws Exception {
        
        System.out.println(">>>>>>>>");

        @SuppressWarnings("unchecked")
        Class<? extends Runnable> cls = (Class<? extends Runnable>)defineClassDynamically(
           OpenPackagesDemo8.class, // Any application class for Java 1.6-10, even from different package
           readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
           OpenPackagesDemo8.class.getProtectionDomain()
       );

        Runnable obj = cls.newInstance();
        obj.run();
        System.out.println("<<<<<<<<");
    }

    private static Class<?> defineClassDynamically(Object moduleOrClass,  
                                                   byte[] classBytes,
                                                   ProtectionDomain pd) throws ClassEmitterException {

        ClassEmitter definer = ClassEmitters.of(moduleOrClass);

        System.out.println("Got definer: " + definer);
        String className = ClassEmitters.classNameOf(classBytes);
        String packageName = className.substring(0, className.lastIndexOf('.'));
        if (null == definer) {
            throw new IllegalArgumentException("No class definer exists for package " + packageName);
        }

        System.out.println("Created definer: " + definer);
        System.out.println("Define class: " + className);
        return definer.defineClass(classBytes, pd);
    }

    private static byte[] readResource(String name) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(name)) {
            int size = 8192;
            byte[] buff = new byte[size];
            int cnt;
            while ((cnt = in.read(buff)) > 0) {
                baos.write(buff, 0, cnt);
            }
        }
        baos.close();
        return baos.toByteArray();
    }
}
