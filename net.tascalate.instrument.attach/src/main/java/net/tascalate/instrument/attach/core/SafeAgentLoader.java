package net.tascalate.instrument.attach.core;

import net.tascalate.instrument.attach.api.AgentLoader;

public interface SafeAgentLoader extends AgentLoader {
    boolean isAvailable();
}
