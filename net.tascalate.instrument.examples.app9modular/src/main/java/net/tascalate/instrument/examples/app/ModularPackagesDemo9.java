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

import net.tascalate.instrument.examples.moduleA.SampleClassA;
import net.tascalate.instrument.examples.moduleB.SampleClassB;
import net.tascalate.instrument.spi.ClassEmitter;
import net.tascalate.instrument.spi.ClassEmitterException;
import net.tascalate.instrument.spi.ClassEmitters;

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
        Module otherModule = classFromModule.getModule();
        System.out.println("Instrumenting module " + otherModule);

        @SuppressWarnings("unchecked")
        Class<? extends Runnable> cls = (Class<? extends Runnable>) 
        defineClassDynamically(otherModule,
                               dynamicClassName,
                               readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
                               SampleClassB.class.getProtectionDomain());

        // Pretty questionable deprecation of API
        @SuppressWarnings("deprecation")
        Runnable obj = cls.newInstance();
        obj.run();
        System.out.println("<<<<<<<<");
    }

    private static Class<?> defineClassDynamically(Module module, 
                                                   String className, 
                                                   byte[] classBytes,
                                                   ProtectionDomain pd) throws ClassEmitterException {

        ClassEmitters.Factory factory = ClassEmitters.of(module, module.getClassLoader());
        /*
         * Effectively, the call above is just the same as ClassDefiners.of(module); Two
         * args form is used to show that same pattern will be used with Java 6-8 and Java
         * 9-11+ from instrumentation agents. Obviously, Java 6-8 agent will use
         * ClassDefiners.of(null, classLoaderArg);
         */

        System.out.println("Get factory: " + factory);

        String packageName = className.substring(0, className.lastIndexOf('.'));
        ClassEmitter definer = factory.create(packageName);
        
        if (null == definer) {
            throw new IllegalArgumentException("No class definer exists for package " + packageName);
        }

        System.out.println("Created definer: " + definer);

        return definer.defineClass(classBytes, pd);
    }

    private static byte[] readResource(String name) throws IOException {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(name).readAllBytes();
    }

}
