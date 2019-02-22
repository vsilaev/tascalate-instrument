package net.tascalate.instrument.spi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
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

    // With Java 9 it's necessary to do a private lookup
    // to the package opened via command line
    private static MethodHandle findDefineClassJ9() throws ReflectiveOperationException {
        return MethodHandles.privateLookupIn(ClassLoader.class, MethodHandles.lookup())
                            .findVirtual(ClassLoader.class,
                                         "defineClass", 
                                         MethodType.methodType(Class.class, 
                                                               String.class, 
                                                               byte[].class, 
                                                               int.class, 
                                                               int.class,
                                                               ProtectionDomain.class));
    }

    static {
        try {
            DEFINE_CLASS = findDefineClassJ9();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

}
