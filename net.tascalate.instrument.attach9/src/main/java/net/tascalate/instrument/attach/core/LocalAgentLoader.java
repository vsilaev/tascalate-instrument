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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.tascalate.instrument.attach.api.AgentLoaderException;

public class LocalAgentLoader extends AbstractAgentLoader implements SafeAgentLoader {

    private final static Logger LOGGER = Logger.getLogger(LocalAgentLoader.class.getName());
    private final boolean forkExternalAttachIfNecessary;
    private final boolean isInsideFork;

    public LocalAgentLoader() {
        this(true);
    }

    public LocalAgentLoader(boolean forkExternalAttachIfNecessary) {
        this.forkExternalAttachIfNecessary = forkExternalAttachIfNecessary;
        this.isInsideFork = false;
    }

    public LocalAgentLoader(File alternativeToolsJar) {
        this.forkExternalAttachIfNecessary = false;
        /* This constructor is the only one used by ExternalAgentLoader */
        this.isInsideFork = true;
    }

    @Override
    public boolean isAvailable() {
        return DELEGATE != null && (IS_SELF_ATTACH_POSSIBLE || forkExternalAttachIfNecessary);
    }

    public void attach(String jarFile, String param) throws IllegalStateException {
        attach(jarFile, param, CurrentProcess.pid());
    }

    void attach(String jarFile, String param, long pid) {
        if (null == DELEGATE) {
            throw new AgentLoaderException(LocalAgentLoader.class.getName() + " is unavailable in current environment", DELEGATE_ERROR);
        }
        long ownPid = CurrentProcess.pid();
        if (ownPid == pid) {
            // Self-attach
            if (!IS_SELF_ATTACH_POSSIBLE) {
                if (forkExternalAttachIfNecessary) {
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer("Forking process for external attach");
                    }
                    new ExternalAgentLoader(null).attach(jarFile, param, ownPid);
                    return;
                } else {
                    throw new AgentLoaderException(
                        "Self-attach is disabled. For JDK 9+ please set system property\"jdk.attach.allowAttachSelf\" to true"
                    );
                }
            }
        }
        
        try {
            DELEGATE.attach(jarFile, param, pid, isInsideFork);
        } catch (AgentLoaderException ex) {
            // Already converted
            throw ex;
        } catch (Exception ex) {
            if (forkExternalAttachIfNecessary) {
                new ExternalAgentLoader(null).attach(jarFile, param, ownPid);
            } else {
                throw new AgentLoaderException(ex);
            }
        }
    }

    boolean isExternalAttachPossible() {
        return true;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[v9, load-method=local-attach, self-attach=" + IS_SELF_ATTACH_POSSIBLE + "]";
    }

    private static final boolean IS_SELF_ATTACH_POSSIBLE;
    private static final VMAttachAPI DELEGATE;
    private static final Throwable DELEGATE_ERROR;

    static {
        IS_SELF_ATTACH_POSSIBLE = Boolean.getBoolean("jdk.attach.allowAttachSelf");
        VMAttachAPI delegate = null;
        Throwable delegateError = null;
        try {
            delegate = new VMAttachImpl();
        } catch (Error ex) {
            delegateError = ex;
        }
        DELEGATE = delegate;
        DELEGATE_ERROR = delegateError;
    }
}