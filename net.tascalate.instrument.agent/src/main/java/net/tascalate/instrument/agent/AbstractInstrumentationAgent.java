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
package net.tascalate.instrument.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInstrumentationAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final String arguments;
    protected final Instrumentation instrumentation;
    
    protected AbstractInstrumentationAgent(String arguments, Instrumentation instrumentation) {
        this.arguments = arguments;
        this.instrumentation = instrumentation;
    }
    
    protected void install() throws Exception {
        log.info("Installing agent...");
        Set<ClassFileTransformer> visited = new HashSet<ClassFileTransformer>();
        for (boolean canRetransform : RETRANSFORM_OPTIONS) {
            for (ClassFileTransformer t : createTransformers(canRetransform)) {
                if (null == t || visited.contains(t)) {
                    continue;
                }
                
                instrumentation.addTransformer(t, canRetransform);
                visited.add(t);
                markTransformerPresence(t);
            }
        }
        markTransformerPresence(this);
        log.info("Agent was installed");
    }
    
    protected void attach(Set<String> ownPackages) throws Exception {
        log.info("Attaching agent dynamically...");

        // Collect classes before ever adding transformer!
        Set<String> extendedOwnPackages = null == ownPackages ? new HashSet<String>() : new HashSet<String>(ownPackages);
        extendedOwnPackages.add(packageNameOf(getClass()) + '.');

        Set<ClassFileTransformer> visited = new HashSet<ClassFileTransformer>();
        boolean hasRetransformableTransformers = false;
        for (boolean canRetransform : RETRANSFORM_OPTIONS) {
            for (ClassFileTransformer t : createTransformers(canRetransform)) {
                if (null == t || visited.contains(t)) {
                    continue;
                }
                
                instrumentation.addTransformer(t, true);
                visited.add(t);
                extendedOwnPackages.add(packageNameOf(t.getClass()) + '.');
                hasRetransformableTransformers |= canRetransform;
            }
        }
        
        if (isSkipRetransformOptionSet()) {
            if (log.isInfoEnabled()) {
                log.info("skipping re-transforming classes according to Java Agent argumentds supplied: " + arguments);
            }
        } else if (!instrumentation.isRetransformClassesSupported()) {
            log.info("JVM does not support re-transform, skipping re-transforming classes");
        } else if (!hasRetransformableTransformers) {
            log.info("No retransformable transformers registered, skipping re-transforming classes");
        } else {
            retransformClasses(extendedOwnPackages);
        }
        for (ClassFileTransformer t : visited) {
            markTransformerPresence(t);
        }
        markTransformerPresence(this);
        log.info("Agent was attached dynamically");
    }
    
    protected void retransformClasses(Set<String> ownPackages) {
        log.info("Re-transforming existing classes...");
        
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            String className = clazz.getName();
            if (instrumentation.isModifiableClass(clazz)) {
                if (isClassLoaderParent(systemClassLoader, clazz.getClassLoader())) {
                    if (log.isTraceEnabled()) {
                        log.trace("Skip re-transforming boot or extension/platform class: " + className);
                    }
                    continue;
                }
                
                boolean isOwnClass = false;
                for (String ownPackage : ownPackages) {
                    if (className.startsWith(ownPackage)) {
                        isOwnClass = true;
                        break;
                    }
                }
                
                if (isOwnClass) {
                    if (log.isDebugEnabled()) {
                        log.debug("Skip re-transforming class (agent class): " + className);
                    }
                    continue;
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("Re-transforming class: " + className);
                }
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (Throwable e) {
                    log.error("Error re-transofrming class "+ className, e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Non-modifiable class (re-transforming skipped): " + className);
                }                    
            }
        }
        log.info("Existing classes was re-transormed");
    }
    
    protected boolean isSkipRetransformOptionSet() {
        return "skip-retransform".equals(arguments);
    }
    
    protected Collection<ClassFileTransformer> createTransformers(boolean canRetransform) {
        return Collections.emptySet();
    }    
    
    protected boolean markTransformerPresence(Object transformer) {
        if (null == transformer) {
            return false;
        }
        String property =
            transformer instanceof IdentifiableInstrumentationObject ?
            ((IdentifiableInstrumentationObject)transformer).id() 
            :
            transformer.getClass().getName();
        
        boolean result;
        if ("true".equalsIgnoreCase(System.getProperty(property, "false"))) {
            if (log.isWarnEnabled()) {
                log.warn("Agent's class \"" + property + "\" presence is already claimed");
            }
            result = false;
        } else {
            result = true;
        }
        System.setProperty(property, "true");
        return result;
    }
    
    protected static int getJdkVersion() {
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
        return javaVersion;
    }
    
    private static String packageNameOf(Class<?> clazz) {
        String className = clazz.getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        } else {
            return null;
        }
    }
    
    private static Set<String> packagePrefixesOf(Class<?>... classes) {
        Set<String> packagePrefixes = new HashSet<String>();
        for (Class<?> clazz : classes) {
            packagePrefixes.add( packageNameOf(clazz) + '.');
        }    
        return packagePrefixes;
    }    
    
    /**
     * Check if <code>maybeParent</code> is a parent (probably indirect) of the <code>classLoader</code>
     * @param classLoader The class loader whose parents are checked, may not be null
     * @param maybeParent Possible parent, may be null for boot class loader
     * @return
     */
    private static boolean isClassLoaderParent(ClassLoader classLoader, ClassLoader maybeParent) {
        ClassLoader cl = classLoader;
        do {
            cl = cl.getParent();
            if (maybeParent == cl) {
                // Check includes null == null for bootstrap classloader
                return true;
            }
        } while (cl != null);
        return false;
    }
    
    protected static final Collection<String> BASE_OWN_PACKAGES = Collections.unmodifiableSet(
        packagePrefixesOf(
            Logger.class, 
            AbstractInstrumentationAgent.class
        )
    );
    
    private static boolean[] RETRANSFORM_OPTIONS = {true, false};
}

