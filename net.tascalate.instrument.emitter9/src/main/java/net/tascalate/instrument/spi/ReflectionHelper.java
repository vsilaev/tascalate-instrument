package net.tascalate.instrument.spi;

import java.io.IOException;
import java.io.InputStream;

class ReflectionHelper {
    private ReflectionHelper() {}
    
    static String getClassName(byte[] bytes) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    static String getClassName(InputStream is) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    static String packageNameOf(String className) {
        throw new UnsupportedOperationException();  
    }
    
    static ClassLoader getBestClassLoader(ClassLoader a, ClassLoader b) {
        throw new UnsupportedOperationException();
    }
    
    static boolean isClassLoaderParent(ClassLoader classLoader, ClassLoader maybeParent) {
        throw new UnsupportedOperationException();
    }
}
