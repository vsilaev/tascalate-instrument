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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class PortableClassFileTransformer implements ClassFileTransformer {
    
    public static interface ClassEmitterFactory {
        ClassEmitter create(boolean mandatory);
    }
    
    private static class ClassEmitterFactoryImpl implements ClassEmitterFactory {
        private final ClassLoader loader;
        public ClassEmitterFactoryImpl(ClassLoader loader) {
            this.loader = loader;
        }
        
        @Override
        public ClassEmitter create(boolean mandatory) {
            return ClassEmitters.of(null, loader, mandatory);
        }        
    }
    
    protected PortableClassFileTransformer(Instrumentation instrumentation) {
        
    }

    @Override
    public final byte[] transform(ClassLoader loader, 
                                  String className, Class<?> classBeingRedefined,
                                  ProtectionDomain protectionDomain, 
                                  byte[] classfileBuffer) throws IllegalClassFormatException {
        return transform(
            resolveClassEmitterFactory(loader), null, loader, 
            className, classBeingRedefined, protectionDomain, classfileBuffer
        );
    }
    
    protected abstract byte[] transform(ClassEmitterFactory emitterFactory,
                                        Object module,
                                        ClassLoader loader,
                                        String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, 
                                        byte[] classfileBuffer) throws IllegalClassFormatException;

    public static byte[] callTransformer(ClassFileTransformer transformer,
                                         Object module,
                                         ClassLoader loader,
                                         String className, Class<?> classBeingRedefined,
                                         ProtectionDomain protectionDomain,
                                         byte[] classfileBuffer) throws IllegalClassFormatException {
        
        if (null != module) {
            throw new IllegalArgumentException("Module parameter for Java versions below 9 should be null");
        }
        return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }
    
    
    private static final Map<ClassLoader, ClassEmitterFactory> CLASS_EMITTER_FACTORY_BY_LOADER = new WeakHashMap<ClassLoader, PortableClassFileTransformer.ClassEmitterFactory>();
    private static ClassEmitterFactory resolveClassEmitterFactory(ClassLoader loader) {
        synchronized (CLASS_EMITTER_FACTORY_BY_LOADER) {
            ClassEmitterFactory result = CLASS_EMITTER_FACTORY_BY_LOADER.get(loader);
            if (null == result) {
                result = new ClassEmitterFactoryImpl(loader);
                CLASS_EMITTER_FACTORY_BY_LOADER.put(loader, result);
            }
            return result;
        }
    }
}
