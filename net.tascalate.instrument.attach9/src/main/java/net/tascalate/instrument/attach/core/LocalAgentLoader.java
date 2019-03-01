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
/**
 * @author vsilaev
 *
 */
package net.tascalate.instrument.attach.core;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import net.tascalate.instrument.attach.api.AgentLoaderException;

public class LocalAgentLoader extends AbstractAgentLoader implements SafeAgentLoader {
	
	private final boolean forkExternalAttachIfNecessary;
	
	public LocalAgentLoader() {
		this(true);
	}
	
	public LocalAgentLoader(boolean forkExternalAttachIfNecessary) {
		this.forkExternalAttachIfNecessary = forkExternalAttachIfNecessary;
	}
	
	public LocalAgentLoader(File alternativeToolsJar) {
		this.forkExternalAttachIfNecessary = false;
	}

	@Override
	public boolean isAvailable() {
	    return IS_SELF_ATTACH_POSSIBLE || forkExternalAttachIfNecessary; 
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
		
		new LocalAgentLoader(alternativeToolsJar).attach(file.getAbsolutePath(), options, pid);
        System.out.println("Completed agent start: " + file.getAbsolutePath() + "=" + options + " @ " + pid);
	}
	
	void attach(String jarFile, String param, long pid) {
		long ownPid = CurrentProcess.pid();
		if (ownPid == pid) {
			// Self-attach
			if (!IS_SELF_ATTACH_POSSIBLE) {
				if (forkExternalAttachIfNecessary) {
					System.out.println("Forking process for external attach");
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
    		VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
    		try {
    		   vm.loadAgent(jarFile, param); 
    		} finally {
    		    vm.detach();
    		}
		} catch (AttachNotSupportedException ex) {
		    if (forkExternalAttachIfNecessary) {
		        new ExternalAgentLoader(null).attach(jarFile, param, ownPid);    
		    }
		} catch (IOException | AgentInitializationException | AgentLoadException ex) {
		    throw new AgentLoaderException("Agent injection not supported on this platform due to unknown reason", ex);
		}
	}
	
	boolean isExternalAttachPossible() {
	    return true;
	}
	
    @Override
    public String toString() {
        return getClass().getName() + "[v9, load-method=local-attach, is-availabel=" + IS_SELF_ATTACH_POSSIBLE + "]"; 
    }
	
	private static final BigDecimal V9 = BigDecimal.valueOf(9);
	
	private static final boolean IS_SELF_ATTACH_POSSIBLE;
	
	static {
        String jvmSpec = System.getProperty("java.vm.specification.version");
        BigDecimal jvmVersion = new BigDecimal(jvmSpec); 
        IS_SELF_ATTACH_POSSIBLE = V9.compareTo(jvmVersion) <= 0 && Boolean.getBoolean("jdk.attach.allowAttachSelf");
	}

}