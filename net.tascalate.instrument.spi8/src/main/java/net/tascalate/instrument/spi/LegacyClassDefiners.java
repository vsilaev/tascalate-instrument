package net.tascalate.instrument.spi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Optional;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

class LegacyClassDefiners implements ClassDefiners {

    private final WeakReference<ClassLoader> classLoaderRef;
    
    private final ClassDefiner definer = new ClassDefiner() {
        @Override
        public Class<?> defineClass(byte[] classBytes, ProtectionDomain protectionDomain) throws ReflectiveOperationException {
            ClassLoader classLoader = classLoaderRef.get();
            if (null == classLoader) {
                throw new IllegalStateException("ClassLoader is unloaded");
            }
            try {
                Class<?> clazz = (Class<?>) DEFINE_CLASS.invokeExact(
                    classLoader, (String) null, classBytes, 0, classBytes.length, protectionDomain
                );
                return clazz;
            } catch (Error | RuntimeException | ReflectiveOperationException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new ReflectiveOperationException(ex);
            }
        }
    };

    LegacyClassDefiners(ClassLoader classLoader) {
        this.classLoaderRef = new WeakReference<>(classLoader);
    }

    @Override
    public Optional<ClassDefiner> lookup(String packageName) throws ReflectiveOperationException {
        return Optional.of(definer);
    }

    private static final MethodHandle DEFINE_CLASS;

    // With Java 8 we may get access to accessible protected method
    private static MethodHandle findDefineClassJ8() throws ReflectiveOperationException {
        Method m = ClassLoader.class.getDeclaredMethod(
            "defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class
        );
        m.setAccessible(true);
        return MethodHandles.lookup().unreflect(m);

    }

    static {
        try {
            DEFINE_CLASS = findDefineClassJ8();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

}
