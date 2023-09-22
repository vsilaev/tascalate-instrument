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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.tascalate.instrument.attach.api.AgentLoader;
import net.tascalate.instrument.attach.api.AgentLoaderException;

public class ExternalAgentLoader extends AbstractAgentLoader implements SafeAgentLoader {

    private final static Logger LOGGER = Logger.getLogger(ExternalAgentLoader.class.getName());
    private final File alternativeToolsJar;

    public ExternalAgentLoader() {
        this(null);
    }

    public ExternalAgentLoader(File alternativeToolsJar) {
        this.alternativeToolsJar = alternativeToolsJar;
    }

    @Override
    public boolean isAvailable() {
        return new LocalAgentLoader(alternativeToolsJar).isExternalAttachPossible();
    }

    @Override
    public void attach(String agentJarPath, String agentParams) {
        attach(agentJarPath, agentParams, CurrentProcess.pid());
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

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting agent " + file.getAbsolutePath() + "=" + options + " @ " + pid + "...");
        }
        new LocalAgentLoader(alternativeToolsJar).attach(file.getAbsolutePath(), options, pid);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Completed agent start: " + file.getAbsolutePath() + "=" + options + " @ " + pid);
        }
    }    

    void attach(String agentJarPath, String agentParams, long pid) {
        try {
            Class<?>[] classes = {
                AgentLoader.class, 
                AgentLoaderException.class, 
                AbstractAgentLoader.class,
                SafeAgentLoader.class, 
                LocalAgentLoader.class, 
                CurrentProcess.class,
                VMAttachAPI.class,
                VMAttachImpl.class
            };
            
            File runnableJar = createJarFile(new HashSet<Class<?>>(Arrays.asList(classes)), ExternalAgentLoader.class);

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Using temporary executable JAR: " + runnableJar.getCanonicalPath());
            }
            try {
                File currentDirectory = new File(".").getCanonicalFile();
                String javaCommand = 
                    System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar +
                    (System.getProperty("os.name", "").toLowerCase(Locale.US).contains("windows") ? "java.exe" : "java");

                String classPath = System.getProperty("java.class.path");
                if (null == classPath || classPath.length() == 0) {
                    classPath = "./";
                }

                List<String> fullCommand = new ArrayList<String>(Arrays.asList(
                    escape(javaCommand), 
                    "-cp", 
                    escape(classPath), 
                    "-jar",
                    escape(runnableJar.getCanonicalPath()), 
                    escape(agentJarPath), 
                    String.valueOf(pid)
                ));
                
                if (null != agentParams && agentParams.length() > 0) {
                    fullCommand.add(escape(agentParams));
                } else if (null != alternativeToolsJar) {
                    fullCommand.add("--");
                }

                if (null != alternativeToolsJar) {
                    fullCommand.add(escape(alternativeToolsJar.getCanonicalPath()));
                }

                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Using Java command: " + fullCommand);
                }

                ProcessBuilder processBuilder = new ProcessBuilder().directory(currentDirectory).command(fullCommand);

                Process process = CurrentProcess.redirectOutputOf(processBuilder)
                                                .redirectErrorStream(true)
                                                .start();

                int ret = process.waitFor();
                if (ret != 0) {
                    throw new IllegalStateException(
                            "Could not self-attach to current VM using external process, exit code is: " + ret);
                } else {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("External attach process completed successfully");
                    }
                }
            } finally {
                runnableJar.delete();
            }
        } catch (InterruptedException ex) {
            throw new IllegalStateException("Could not self-attach to current VM using external process", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not self-attach to current VM using external process", ex);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[load-method=fork-external-attach]";
    }

    private static File createJarFile(Set<Class<?>> classes, Class<?> mainClass) throws IOException {
        File agentJar = File.createTempFile("tal-ext", ".jar");
        agentJar.deleteOnExit();

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass.getName());
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(agentJar), manifest);
        Set<Class<?>> allClasses = new HashSet<Class<?>>(classes);
        allClasses.add(mainClass);
        try {
            for (Class<?> clazz : allClasses) {
                String resourceName = clazz.getName().replace('.', '/') + ".class";
                jarOutputStream.putNextEntry(new JarEntry(resourceName));
                byte[] buffer = new byte[8192];
                int size;
                InputStream inputStream = ExternalAgentLoader.class.getResourceAsStream('/' + resourceName);
                try {
                    while ((size = inputStream.read(buffer)) > 0) {
                        jarOutputStream.write(buffer, 0, size);
                    }
                    jarOutputStream.closeEntry();
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException exIgnore) {

                    }
                }
            }
        } finally {
            jarOutputStream.close();
        }
        return agentJar;
    }

    private static String escape(String value) {
        return value.contains(" ") ? '"' + value + '"' : value;
    }

}
