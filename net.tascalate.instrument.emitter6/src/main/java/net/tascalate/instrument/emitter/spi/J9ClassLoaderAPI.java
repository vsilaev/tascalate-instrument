package net.tascalate.instrument.emitter.spi;

import java.security.ProtectionDomain;

/**
 * Placeholder class -- will be replaced with the code from *.emitter9
 * @author vsilaev
 *
 */
class J9ClassLoaderAPI extends ClassLoaderAPI {

    @Override
    Object getClassLoadingLock(ClassLoader classLoader, String className) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Package getPackage(ClassLoader classLoader, String packageName) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Package definePackage(ClassLoader classLoader, String packageName) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Class<?> findLoadedClass(ClassLoader classLoader, String className) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    Class<?> defineClass(ClassLoader classLoader, String className, byte[] classBytes, ProtectionDomain protectionDomain) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    int version() {
        throw new UnsupportedOperationException();
    }
}