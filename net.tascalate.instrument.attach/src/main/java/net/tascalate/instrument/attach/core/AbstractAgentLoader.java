package net.tascalate.instrument.attach.core;

import net.tascalate.instrument.attach.api.AgentLoader;

abstract class AbstractAgentLoader implements AgentLoader {
    
    public final void attach(String agentJarPath) {
        attach(agentJarPath, null);
    }
    
}
