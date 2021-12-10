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
package net.tascalate.instrument.examples.app;

import java.io.IOException;
import java.security.ProtectionDomain;

import net.tascalate.instrument.emitter.spi.ClassEmitter;
import net.tascalate.instrument.emitter.spi.ClassEmitterException;
import net.tascalate.instrument.emitter.spi.ClassEmitters;
import net.tascalate.instrument.examples.moduleA.SampleClassA;
import net.tascalate.instrument.examples.moduleB.SampleClassB;

public class ModularPackagesDemo9 {

    public static void main(String[] args) throws Throwable {
        System.out.println("Hello");

        Module myModule = ModularPackagesDemo9.class.getModule();
        System.out.println("App is running in module " + myModule.getName());

        demoModule(SampleClassA.class, "net.tascalate.instrument.examples.moduleA.internalY.GuttenTagModuleA");

        demoModule(SampleClassB.class, "net.tascalate.instrument.examples.moduleB.internalX.HelloModuleB");
    }

    static void demoModule(Class<?> classFromModule, 
                           String dynamicClassName) throws Exception {
        
        System.out.println(">>>>>>>>");
        Module myModule = classFromModule.getModule();
        System.out.println("Instrumenting module " + myModule);

        @SuppressWarnings("unchecked")
        Class<? extends Runnable> cls = (Class<? extends Runnable>) 
        defineClassDynamically(myModule,
                               readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
                               classFromModule.getProtectionDomain());

        Runnable obj = cls.getConstructor().newInstance();
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

        System.out.println("Define class: " + className);
        return definer.defineClass(classBytes, pd);
    }

    private static byte[] readResource(String name) throws IOException {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(name).readAllBytes();
    }

}
