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
            messages.add("Native (JNI) Agent injection only works with a Sun-derived VM 1.6 or higher (no class " + className + " found)");
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
