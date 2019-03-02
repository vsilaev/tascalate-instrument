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

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Locale;

final class CurrentProcess {
    private CurrentProcess() {
    }

    static long pid() {
        String pid = null;
        if (!IS_WINDOWS_OS) {
            final File self = new File("/proc/self");
            try {
                if (self.exists()) {
                    pid = self.getCanonicalFile().getName();
                }
            } catch (IOException ignored) {
            }
        }

        if (pid == null) {
            String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            pid = name.substring(0, name.indexOf('@'));
        }

        return Long.parseLong(pid);
    }
    
    static ProcessBuilder redirectOutputOf(ProcessBuilder processBuilder) {
        if (null == REDIRECT_OUTPUT) {
            return processBuilder;
        } else {
            try {
                return (ProcessBuilder) REDIRECT_OUTPUT.invoke(processBuilder, REDIRECT_INHERIT);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }        
    }

    private static final boolean IS_WINDOWS_OS = 
        System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
    

    private static final Method REDIRECT_OUTPUT;
    private static final Object REDIRECT_INHERIT;

    static {
        Class<?> processBuilderClass = ProcessBuilder.class;
        Method redirectOutput = null;
        Object redirectInherit = null;
        try {
            Class<?> redirectClass = Class.forName("java.lang.ProcessBuilder$Redirect");

            Field redirectInheritFiled = redirectClass.getField("INHERIT");
            redirectInherit = redirectInheritFiled.get(null);

            redirectOutput = processBuilderClass.getMethod("redirectOutput", redirectClass);
        } catch (ClassNotFoundException ex) {
            // Pitty, Java version is below 1.7
        } catch (NoSuchMethodException ex) {
            // Same reason
        } catch (NoSuchFieldException ex) {
            // Same reason
        } catch (IllegalAccessException ex) {
            // Should not happen
            throw new RuntimeException(ex);
        }
        REDIRECT_OUTPUT = redirectOutput;
        REDIRECT_INHERIT = redirectInherit;
    }
}
