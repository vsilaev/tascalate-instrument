package net.tascalate.instrument.examples.moduleB;

public class SampleClassB implements Runnable {
    public void run() {
        System.out.println(
           "Hello,\nI'm \"" + getClass().getName() + 
           "\",\ndefined in module ~" + getClass().getModule().getName() + 
           "~\nand loaded by " + getClass().getClassLoader() + " class-loader" +
           ",\nmy superclass is " + getClass().getSuperclass()
        );
    }
}

