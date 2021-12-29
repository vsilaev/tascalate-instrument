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
package net.tascalate.instrument.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public abstract class AbstractLambdaAwareInstrumentationAgent extends AbstractInstrumentationAgent {

    protected AbstractLambdaAwareInstrumentationAgent(String arguments, Instrumentation instrumentation) {
        super(arguments, instrumentation);
    }
    
    protected boolean attachLambdaInstrumentationHook(LambdaClassTransformer hook) throws Exception {
        int jdkVersion = getJdkVersion(); 
        if (jdkVersion < 9) {
            if (log.isDebugEnabled()) {
                log.debug("JDK verion " + jdkVersion + " is less than 9, no lambda instrumentation patch required.");
            }
            return false;
        }
        
        // Need to patch lambda metafactory to support agent instrumentation for lambda classes
        // in JDK 9+
        try {
            if (!RuntimeBytecodeInjector.isInjectionApplied()) {
                log.debug("Applying lambda post-processing injection...");
                instrumentation.redefineClasses(RuntimeBytecodeInjector.modifyLambdaMetafactory());
                log.debug("Lambda post-processing injection is applied.");
            } else {
                log.warn("Lambda post-processing injection was already applied, probably by some other agent.");
            }
            RuntimeBytecodeInjector.installTransformer(hook);
            return true;
        } catch (Error ex) {
            log.warn("Unable to apply lambda instrumentation patch (unsupported JVM)", ex);
            throw ex;
        } catch (Exception ex) {
            log.warn("Unable to apply lambda instrumentation patch (unsupported JVM)", ex);
            throw ex;
        }
    }
    
    protected LambdaClassTransformer createLambdaClassTransformer(final ClassFileTransformer transformer) {
        return new LambdaClassTransformer() {
            @Override
            public byte[] transform(Class<?> lambdaOwningClass, byte[] lambdaClassBytes) throws Throwable {
                return transformLambdaClass(transformer, lambdaOwningClass, lambdaClassBytes);
            }
        };
    }
    
    protected String readLambdaClassName(byte[] bytes) throws Exception {
        return "<unknown-lambda>";
    }
    
    byte[] transformLambdaClass(ClassFileTransformer classFileTransformer, Class<?> lambdaOwningClass, byte[] input) throws Throwable {
        try {
            String className = null;            
            if (log.isDebugEnabled()) {
                className = readLambdaClassName(input);
                log.debug("Start transforming lambda class " + className + ", defined in " + lambdaOwningClass.getName());
            }
            byte[] output = classFileTransformer.transform(lambdaOwningClass.getClassLoader(), 
                                                           null, null, // Neither name, nor class
                                                           lambdaOwningClass.getProtectionDomain(), 
                                                           input);
            if (log.isDebugEnabled()) {
                log.debug("Done transforming lambda class " + className + 
                           ", defined in " + lambdaOwningClass.getName() + 
                           ", modified = " + (input != output && output != null));
            }                
            return output;
        } catch (Throwable ex) {
            log.error("Error transforming lambda class defined in " + lambdaOwningClass.getName(), ex);
            throw ex;
        }
        
    }
    
}
