/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2019-2022, Valery Silaev
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
package net.tascalate.instrument.attach.core;

import java.io.IOException;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import net.tascalate.instrument.attach.api.AgentLoaderException;

class VMAttachImpl extends VMAttachAPI {
    @Override
    void attach(String jarFile, String param, long pid, boolean isInsideFork) throws Exception {
        try {
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
            try {
                if (isInsideFork) {
                    // Spawn attach in separate thread to avoid 
                    // having library classes on stack -- 
                    // otherwise it may cause an error in transformers
                    Throwable[] errorRef = new Throwable[1];
                    new Thread(() -> {
                        try {
                            vm.loadAgent(jarFile, param);
                        } catch (Error | Exception ex) {
                            errorRef[0] = ex;
                        }
                    }).join();
                    Throwable error = errorRef[0];
                    if (error instanceof Error) {
                        throw (Error)error;
                    } else if (null != error) {
                        throw (Exception)error;
                    }
                } else {
                    vm.loadAgent(jarFile, param);
                }
            } finally {
                vm.detach();
            }
        } catch (AttachNotSupportedException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new AgentLoaderException("Agent injection not supported on this platform due to unknown reason", ex);
        } catch (AgentInitializationException | AgentLoadException ex) {
            throw new AgentLoaderException("Internal error in Java Agent", ex);
        }
    }
   
}
