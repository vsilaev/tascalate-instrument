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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import net.tascalate.instrument.attach.api.AgentLoader;
import net.tascalate.instrument.attach.api.AgentLoaderException;

class NativeAgentLoader extends AbstractAgentLoader {
    /**
     * This interface is used internally by the {@code AgentLoader} to interface
     * with the VM core.
     */
    static interface LibInstrument extends Library {
        void Agent_OnAttach(Pointer vm, String name, Pointer Reserved);
    }

    /**
     * This interface is used internally by the {@code AgentLoader} to interface
     * with the VM core.
     */
    static interface LibJVM extends Library {
        int JNI_GetCreatedJavaVMs(PointerByReference vms, int count, IntByReference found);
    }

    public void attach(String agentJarPath, String agentParams) {
        PointerByReference vms = new PointerByReference();
        IntByReference found = new IntByReference();
        LIB_JVM.JNI_GetCreatedJavaVMs(vms, 1, found);

        if (found.getValue() == 0) {
            throw new AgentLoaderException("Failed to access current VirtualMachine via JNI");
        }

        Pointer vm = vms.getValue();
        StringBuilder options = new StringBuilder(agentJarPath);
        if (null != agentParams) {
            options.append('=').append(agentParams);
        }
        LIB_INSTRUMENT.Agent_OnAttach(vm, options.toString(), null);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[load-method=JNA(JNI)]";
    }

    static final AgentLoader INSTANCE = new NativeAgentLoader();

    private static final LibJVM LIB_JVM = Native.load(LibJVM.class);
    private static final LibInstrument LIB_INSTRUMENT = Native.load(LibInstrument.class);
}
