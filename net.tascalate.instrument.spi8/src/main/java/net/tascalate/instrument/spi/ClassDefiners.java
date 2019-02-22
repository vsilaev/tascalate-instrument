package net.tascalate.instrument.spi;

import java.util.Objects;
import java.util.Optional;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

public interface ClassDefiners {

    /**
     * Lookup for a {@link ClassDefiner} for the given package.
     * 
     * @param packageName the name of the package (com.company.subpackage) where
     *                    classes will be defined
     * @return the {@link Optional} containing the {@link ClassDefiner} used to
     *         dynamically create classes inside the given package, or empty if the
     *         package is not "open"
     * @throws ReflectiveOperationException
     */
    abstract Optional<ClassDefiner> lookup(String packageName) throws ReflectiveOperationException;

    public static ClassDefiners of(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader);
        return new LegacyClassDefiners(classLoader);
    }

    public static ClassDefiners of(Object module, ClassLoader classLoader) {
        if (null != module) {
            String moduleClassName = "java.lang.Module"; 
            if (!moduleClassName.equals(module)) {
                throw new IllegalArgumentException("First parameter should be either null or instance of " + moduleClassName);
            }
        }
        return ClassDefiners.of(classLoader);
    }
}
