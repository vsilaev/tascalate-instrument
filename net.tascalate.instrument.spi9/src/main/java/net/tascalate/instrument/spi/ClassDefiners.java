package net.tascalate.instrument.spi;

import java.util.Objects;
import java.util.Optional;

import net.tascalate.instrument.api.AbstractOpenPackage;
import net.tascalate.instrument.api.AllowDynamicClasses;
import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;
import net.tascalate.instrument.spi.EmptyClassDefiners;

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

    public static ClassDefiners of(Module module) {
        return of(module, false);
    }

    public static ClassDefiners of(Module module, boolean mandatory) {
        Objects.requireNonNull(module);
        AllowDynamicClasses settings = module.getAnnotation(AllowDynamicClasses.class);
        if (null == settings) {
            if (mandatory) {
                throw new IllegalStateException(
                    "Module " + module.getName() + " does not allow defying dynamic classes " + 
                    "(not annotated with " + AllowDynamicClasses.class.getName() + ")"
                );
            } else {
                return EmptyClassDefiners.INSTANCE;
            }
        }

        Class<? extends AbstractOpenPackage>[] packageClasses = settings.value();
        if (packageClasses.length == 0) {
            // TODO: Or error?
            return EmptyClassDefiners.INSTANCE;
        }

        Module selfModule = ClassDefiners.class.getModule();
        if (!selfModule.canRead(module)) {
            // Obviously, we unaware initially of all modules that requires
            // dynamic class definitions.
            // So it's necessary to add "read" access dynamically
            selfModule.addReads(module);
        }

        return new DefaultClassDefiners(module, packageClasses);
    }

    public static ClassDefiners of(Object module, ClassLoader classLoader) {
        if (null != module) {
            try {
                return of((Module)module, true);
            } catch (IllegalStateException ex) {
                // TODO: should propagate? access to classLoader is banned 
            }
        }
        if (null == classLoader && null != module) {
            classLoader = ((Module)module).getClassLoader();
        }
        return ClassDefiners.of(classLoader);
    }
}
