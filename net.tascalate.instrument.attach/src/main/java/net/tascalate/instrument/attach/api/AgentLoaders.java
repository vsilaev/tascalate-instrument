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
package net.tascalate.instrument.attach.api;

import java.io.File;

import net.tascalate.instrument.attach.core.ExternalAgentLoader;
import net.tascalate.instrument.attach.core.LocalAgentLoader;
import net.tascalate.instrument.attach.core.SafeAgentLoader;
import net.tascalate.instrument.attach.core.SafeAgentLoaderChain;
import net.tascalate.instrument.attach.core.SafeNativeAgentLoader;

public final class AgentLoaders {

    private AgentLoaders() {
    }

    public static AgentLoader getDefault() {
        for (SafeAgentLoader loader : new SafeAgentLoaderChain()) {
            if (loader.isAvailable()) {
                return new WithArgsCheck(loader);
            }
        }
        throw new AgentLoaderException("No suitable Java Agent loader in current environment");
    }

    public static AgentLoader getNative() {
        return new SafeNativeAgentLoader();
    }

    public static AgentLoader getLocal() {
        return new LocalAgentLoader(false);
    }

    public static AgentLoader getExternal() {
        return new ExternalAgentLoader();
    }

    public static void attach(String jarFile, String param) throws AgentLoaderException {
        getDefault().attach(jarFile, param);
    }
    
    static class WithArgsCheck implements AgentLoader {
        private final AgentLoader loader;
        
        WithArgsCheck(AgentLoader loader) {
            this.loader = loader;
        }

        @Override
        public void attach(String agentJarPath) {
            checkFile(agentJarPath);
            loader.attach(agentJarPath);
        }

        @Override
        public void attach(String agentJarPath, String agentParams) {
            checkFile(agentJarPath);
            loader.attach(agentJarPath, agentParams);
        }
        
        @Override
        public String toString() {
            return loader.toString() + "**checking-file**";
        }
        
        private void checkFile(String agentJarPath) {
            File f = new File(agentJarPath);
            if (!f.isFile() || !f.canRead()) {
                throw new IllegalArgumentException(
                    "Cannot access Java Agent JAR file: " + agentJarPath
                );
            }
        }
    }
}
