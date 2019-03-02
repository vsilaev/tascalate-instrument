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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tascalate.instrument.attach.api.AgentLoaderException;

public class LocalAgentLoader extends AbstractAgentLoader implements SafeAgentLoader {

    @SuppressWarnings("unused")
    private final File alternativeToolsJar;
    private final Method vmAttach;
    private final Method vmLoadAgent;
    private final Method vmDetach;

    public LocalAgentLoader() {
        this(true);
    }

    public LocalAgentLoader(boolean forkExternalAttachIfNecessary) {
        alternativeToolsJar = null;
        vmAttach    = VM_ATTACH_METHOD;
        vmLoadAgent = VM_LOAD_AGENT_METHOD;
        vmDetach    = VM_DETACH_METHOD;
    }

    public LocalAgentLoader(File alternativeToolsJar) {
        this.alternativeToolsJar = alternativeToolsJar;

        Method[] m;
        if (null != alternativeToolsJar && null == VM_ATTACH_METHOD) {

            m = new Method[3];
            try {
                ClassLoader classLoader = new URLClassLoader(
                    new URL[] {alternativeToolsJar.toURI().toURL()}, getClass().getClassLoader()
                );
                
                Class<?> clazz = tryLoadVmClass(classLoader);
                if (null != clazz) {
                    m = tryGetVmMethods(clazz);
                }
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException("Unable to access file " + alternativeToolsJar);
            }
        } else {
            m = new Method[] { VM_ATTACH_METHOD, VM_LOAD_AGENT_METHOD, VM_DETACH_METHOD };
        }
        vmAttach    = m[0];
        vmLoadAgent = m[1];
        vmDetach    = m[2];
    }

    @Override
    public boolean isAvailable() {
        return vmAttach != null;
    }

    public void attach(String jarFile, String param) throws IllegalStateException {
        attach(jarFile, param, CurrentProcess.pid());
    }

    public static void main(String[] argv) throws IOException {
        if (argv == null || argv.length < 2) {
            System.out.println("Invalid arguments, ussage:");
            System.out.println("java " + LocalAgentLoader.class.getName() + " <agent.jar> <process-id>");
            System.exit(-1);
        }

        File file = new File(argv[0]).getCanonicalFile();
        if (!file.exists() || !file.canRead() || !file.isFile()) {
            System.out.println("Agent archive file does not exist or not accessible: " + file.getAbsolutePath());
            System.exit(-2);
        }

        long pid = Long.valueOf(argv[1]);
        String options = argv.length > 2 && !"--".equals(argv[2]) ? argv[2] : null;
        File alternativeToolsJar = argv.length > 3 ? new File(argv[3]) : null;

        System.out.println("Starting agent " + file.getAbsolutePath() + "=" + options + " @ " + pid + "...");
        new LocalAgentLoader(alternativeToolsJar).attach(file.getAbsolutePath(), options, pid);
        System.out.println("Completed agent start: " + file.getAbsolutePath() + "=" + options + " @ " + pid);
    }

    void attach(String jarFile, String param, long pid) {
        if (null == vmAttach) {
            throw new AgentLoaderException(ERROR_MESSAGES);
        }

        Throwable exception = null;
        try {
            Object vm = vmAttach.invoke(null, String.valueOf(pid));
            try {
                vmLoadAgent.invoke(vm, jarFile, param);
            } finally {
                vmDetach.invoke(vm);
            }
        } catch (InvocationTargetException ex) {
            exception = ex.getCause();
            if (exception == null) {
                exception = ex;
            }
        } catch (Error ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            exception = t;
        }

        if (exception != null) {
            throw new AgentLoaderException(
                "Agent injection not supported on this platform due to unknown reason", exception
            );
        }
    }

    boolean isExternalAttachPossible() {
        return vmAttach != null;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[v8, load-method=local-attach, is-availabel=" + (vmAttach != null) + "]";
    }

    private static final Method VM_ATTACH_METHOD;
    private static final Method VM_LOAD_AGENT_METHOD;
    private static final Method VM_DETACH_METHOD;

    private static final List<String> ERROR_MESSAGES;

    private static final String[] VM_CLASS_NAMES = { 
        "com.sun.tools.attach.VirtualMachine",
        "com.ibm.tools.attach.VirtualMachine" 
    };

    private static Class<?> tryLoadVmClass(ClassLoader classLoader) {
        for (String className : VM_CLASS_NAMES) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
            }
        }
        return null;
    }

    private static Method[] tryGetVmMethods(Class<?> vmClass) {
        Method[] result = new Method[3];
        try {
            result[0] = vmClass.getMethod("attach", String.class);
        } catch (NoSuchMethodException ex) {
        }

        try {
            result[1] = vmClass.getMethod("loadAgent", String.class, String.class);
        } catch (NoSuchMethodException ex) {
        }

        try {
            result[2] = vmClass.getMethod("detach");
        } catch (NoSuchMethodException ex) {
        }

        return result;
    }

    static {
        List<String> messages = new ArrayList<String>();

        String[] classPathExtras = { null, "lib/tools.jar", "../lib/tools.jar", "../Classes/classes.jar" };
        ClassLoader baseClassLoader = LocalAgentLoader.class.getClassLoader();
        File basePath = null;

        Class<?> vmClass = null;
        for (int i = 0; i < classPathExtras.length; i++) {
            ClassLoader classLoader;
            String extra = classPathExtras[i];
            if (null != extra) {
                try {
                    if (null == basePath) {
                        basePath = new File(System.getProperty("java.home"));
                        if (!(basePath.exists() && basePath.canRead())) {
                            messages.add("Cannot access path JAVA_HOME=" + basePath.getCanonicalPath());
                            break;
                        }
                    }
                    File jarPath = new File(basePath, extra);
                    if (jarPath.exists() && jarPath.canRead()) {
                        classLoader = new URLClassLoader(new URL[] { jarPath.toURI().toURL() }, baseClassLoader);
                    } else {
                        continue;
                    }
                } catch (IOException ex) {
                    continue;
                }
            } else {
                classLoader = baseClassLoader;
            }
            vmClass = tryLoadVmClass(classLoader);
            if (null != vmClass) {
                break;
            }
        }
        if (null != vmClass) {
            Method[] methods = tryGetVmMethods(vmClass);
            VM_ATTACH_METHOD = methods[0];
            if (null == VM_ATTACH_METHOD) {
                messages.add("Cannot get VirtualMachine.attach method");
            }

            VM_LOAD_AGENT_METHOD = methods[1];
            if (null == VM_LOAD_AGENT_METHOD) {
                messages.add("Cannot get VirtualMachine.loadAgent method");
            }

            VM_DETACH_METHOD = methods[2];
            if (null == VM_DETACH_METHOD) {
                messages.add("Cannot get VirtualMachine.detach method");
            }

        } else {
            messages.add("Cannot get VirtualMachine class");
            VM_ATTACH_METHOD     = null;
            VM_LOAD_AGENT_METHOD = null;
            VM_DETACH_METHOD     = null;
        }
        ERROR_MESSAGES = Collections.unmodifiableList(messages);
    }

}