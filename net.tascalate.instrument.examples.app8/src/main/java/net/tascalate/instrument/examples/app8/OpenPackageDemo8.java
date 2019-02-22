package net.tascalate.instrument.examples.app8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

import net.tascalate.instrument.spi.ClassDefiner;
import net.tascalate.instrument.spi.ClassDefiners;

public class OpenPackageDemo8 {
    
    public static void main(String[] args) throws Throwable {
        System.out.println("Hello");

        System.out.println("App is running");

        demoModule("net.tascalate.instrument.examples.app8.surprise.Demo");
    }

    static void demoModule(String dynamicClassName) throws ReflectiveOperationException, IOException {
        
        System.out.println(">>>>>>>>");

        @SuppressWarnings("unchecked")
        Class<? extends Runnable> cls = (Class<? extends Runnable>) 
        defineClassDynamically(null, // No module, ya!
                               ClassLoader.getSystemClassLoader(), 
                               dynamicClassName,
                               readResource(dynamicClassName.substring(dynamicClassName.lastIndexOf('.') + 1) + ".bytes"),
                               OpenPackageDemo8.class.getProtectionDomain());

        Runnable obj = cls.newInstance();
        obj.run();
        System.out.println("<<<<<<<<");
    }

    private static Class<?> defineClassDynamically(Object module, 
                                                   ClassLoader classLoader, 
                                                   String className, 
                                                   byte[] classBytes,
                                                   ProtectionDomain pd) throws ReflectiveOperationException {

        ClassDefiners definers = ClassDefiners.of(module, classLoader);
        /*
         * Effectively, the call above is just the same as ClassDefiners.of(classLoader); 
         * Two args form is used to show that same pattern will be used with Java 8 and 
         * Java 9+ from instrumentation agents.
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(name)) {
            int size = 8192;
            byte[] buff = new byte[size];
            int cnt;
            while ((cnt = in.read(buff)) > 0) {
                baos.write(buff, 0, cnt);
            }
        }
        baos.close();
        return baos.toByteArray();
    }
}
