package net.tascalate.instrument.examples.app;

import java.io.IOException;
import java.security.ProtectionDomain;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

public class SimpleOpenPackagesDemo9 {

    public static void main(String[] args) throws Throwable {
        System.out.println("Hello");

        Module myModule = SimpleOpenPackagesDemo9.class.getModule();
        System.out.println("App is running in module " + myModule.getName());

        demoModule("net.tascalate.instrument.examples.app.dynamic.Demo");
    }

    static void demoModule(String dynamicClassName) throws ReflectiveOperationException, IOException {
        
        System.out.println(">>>>>>>>");
        Module otherModule = SimpleOpenPackagesDemo9.class.getModule();
        System.out.println("Instrumenting module " + otherModule);

        @SuppressWarnings("unchecked")
        Class<? extends Runnable> cls = (Class<? extends Runnable>) 
        defineClassDynamically(otherModule,
                               dynamicClassName,
                               readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
                               SimpleOpenPackagesDemo9.class.getProtectionDomain());

        @SuppressWarnings("deprecation")
        // Oracle is getting insane about warnings
        Runnable obj = cls.newInstance();
        obj.run();
        System.out.println("<<<<<<<<");
    }

    private static Class<?> defineClassDynamically(Module module, 
                                                   String className, 
                                                   byte[] classBytes,
                                                   ProtectionDomain pd) throws ReflectiveOperationException {

        ClassDefiners definers = ClassDefiners.of(module, module.getClassLoader());
        /*
         * Effectively, the call above is just the same as ClassDefiners.of(module); Two
         * args form is used to show that same pattern will be used with Java 8 and Java
         * 9+ from instrumentation agents. Obviously, Java 8 agent will use
         * ClassDefiners.of(null, classLoaderArg);
         */

        System.out.println("Get Definers: " + definers);

        String packageName = className.substring(0, className.lastIndexOf('.'));

        ClassDefiner definer = definers
                .lookup(packageName)
                .orElseThrow(() -> new IllegalArgumentException("No class definer exists for package " + packageName));

        System.out.println("Created definer: " + definer);

        return definer.defineClass(classBytes, pd);
    }

    private static byte[] readResource(String name) throws IOException {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(name).readAllBytes();
    }

}
