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
           null, // No module, ya!
           ClassLoader.getSystemClassLoader(), 
           dynamicClassName,
           readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
           OpenPackagesDemo8.class.getProtectionDomain()
       );

        Runnable obj = cls.newInstance();
        obj.run();
        System.out.println("<<<<<<<<");
    }

    private static Class<?> defineClassDynamically(Object module, 
                                                   ClassLoader classLoader, 
                                                   String className, 
                                                   byte[] classBytes,
                                                   ProtectionDomain pd) throws ClassEmitterException {

        ClassEmitters.Factory factory = ClassEmitters.of(module, classLoader);
        /*
         * Effectively, the call above is just the same as ClassDefiners.of(classLoader); 
         * Two args form is used to show that same pattern will be used with Java 6-8 and 
         * Java 9-11+ from instrumentation agents.
         */

        System.out.println("Get factory: " + factory);

        String packageName = className.substring(0, className.lastIndexOf('.'));

        ClassEmitter definer = factory.create(packageName);
        if (null == definer) {
            throw new IllegalArgumentException("No class definer exists for package " + packageName);
        }

        System.out.println("Created definer: " + definer);
        System.out.println("Define class: " + ClassEmitters.classNameOf(classBytes));
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
