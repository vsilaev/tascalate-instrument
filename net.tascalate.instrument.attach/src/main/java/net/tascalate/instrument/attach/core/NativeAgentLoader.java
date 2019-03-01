package net.tascalate.instrument.attach.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import net.tascalate.instrument.attach.api.AgentLoader;

class NativeAgentLoader extends AbstractAgentLoader {
	/**
	 * This interface is used internally by the {@code AgentLoader} to interface with the VM core.
	 */
	static interface LibInstrument extends Library {
		void Agent_OnAttach(Pointer vm, String name, Pointer Reserved);
	}
	
	/**
	 * This interface is used internally by the {@code AgentLoader} to interface with the VM core.
	 */
	static interface LibJVM extends Library {
		int JNI_GetCreatedJavaVMs(PointerByReference vms, int count, IntByReference found);
	}
	
	public void attach(String agentJarPath, String agentParams) throws IllegalStateException {
		PointerByReference vms = new PointerByReference();
		IntByReference found = new IntByReference();
		LIB_JVM.JNI_GetCreatedJavaVMs(vms, 1, found);
		
		if (found.getValue() == 0) {
			throw new IllegalStateException("Failed to access current VirtualMachine via JNI");
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

    LibJVM LIB_JVM = Native.load(LibJVM.class); 
    LibInstrument LIB_INSTRUMENT = Native.load(LibInstrument.class);
}
