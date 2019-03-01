package net.tascalate.instrument.attach.core;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

final class CurrentProcess {
	private CurrentProcess() {}
	
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
	
	private static final boolean IS_WINDOWS_OS = System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
}
