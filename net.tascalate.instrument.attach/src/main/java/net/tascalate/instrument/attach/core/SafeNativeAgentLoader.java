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
package net.tascalate.instrument.attach.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tascalate.instrument.attach.api.AgentLoaderException;

public class SafeNativeAgentLoader extends AbstractAgentLoader implements SafeAgentLoader {

    @Override
    public boolean isAvailable() {
        return ERROR_MESSAGES.isEmpty();
    }

    @Override
    public void attach(String agentJarPath, String agentParams) {
        if (ERROR_MESSAGES.isEmpty()) {
            NativeAgentLoader.INSTANCE.attach(agentJarPath, agentParams);
        } else {
            throw new AgentLoaderException(ERROR_MESSAGES);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "@@@" + NativeAgentLoader.INSTANCE.toString();
    }

    private static final List<String> ERROR_MESSAGES;

    static {
        List<String> messages = new ArrayList<String>();
        String className;

        className = "sun.instrument.InstrumentationImpl";
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            messages.add(
                "Native (JNI) Agent injection only works with a Sun-derived VM 1.6 or higher " +
                "(no class " + className + " found)"
            );
        }

        className = "com.sun.jna.Library";
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            messages.add("No JNA library present on classpath");
        }

        ERROR_MESSAGES = Collections.unmodifiableList(messages);
    }
}
