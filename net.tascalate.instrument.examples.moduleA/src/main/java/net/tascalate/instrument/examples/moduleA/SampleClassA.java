package net.tascalate.instrument.examples.moduleA;

public class SampleClassA implements Runnable {
    public void run() {
        System.out.println(
            "Gutten Tag!\nI'm '" + getClass().getName() + 
            "',\ndefined in module #" + getClass().getModule().getName() + 
            "#\nand loaded by " + getClass().getClassLoader() + " class-loader" +
            ",\nmy superclass is " + getClass().getSuperclass()
         );
    }
}
