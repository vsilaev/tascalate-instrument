package net.tascalate.instrument.examples.app;

public class SayHello implements Runnable {
    public void run() {
        System.out.println(
           "Hello, I'm " + getClass().getName() + 
           ", defined in module " + getClass().getModule() + 
           " and loaded by " + getClass().getClassLoader() + " class-loader"
        );
    }
}
