package net.tascalate.instrument.spi;

import java.security.ProtectionDomain;

/**
 * <p>The contract to define classes dynamically.
 * 
 * @author vsilaev
 * @see ClassDefiners
 */
@FunctionalInterface
public interface ClassDefiner {
	Class<?> defineClass(byte[] classBytes, ProtectionDomain protectionDomain) throws ReflectiveOperationException;
}
