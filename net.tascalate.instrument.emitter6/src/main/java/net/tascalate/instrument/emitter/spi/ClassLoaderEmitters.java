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

class ClassLoaderEmitters implements ClassEmitters.Factory {

    private final ClassLoaderEmitter emitter;

    ClassLoaderEmitters(ClassLoader classLoader) {
        emitter = new ClassLoaderEmitter(classLoader, CLASS_LOADER_API);
    }

    @Override
    public ClassEmitter create(String packageName) {
        return emitter;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[method=reflection, supported-packages=<any>, " + emitter.describe() + "]"; 
    }
    
    private static final ClassLoaderAPI CLASS_LOADER_API;
    
    static {
        String version = System.getProperty("java.version");
        
        if (version.startsWith("1.")) {
            version = version.substring(2, version.indexOf('.', 2));
        } else {
            int dot = version.indexOf(".");
            if (dot > 0) { 
                version = version.substring(0, dot); 
            }
        } 
        int javaVersion = Integer.parseInt(version);
        if (javaVersion < 7) {
            CLASS_LOADER_API = new J6ClassLoaderAPI();
        } else if (javaVersion < 9) {
            CLASS_LOADER_API = new J7ClassLoaderAPI();
        } else {
            CLASS_LOADER_API = new J9ClassLoaderAPI();
        }
    }
}
