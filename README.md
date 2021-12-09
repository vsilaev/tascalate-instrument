[![Maven Central](https://img.shields.io/maven-central/v/net.tascalate.instrument/net.tascalate.instrument.parent.svg)](https://search.maven.org/artifact/net.tascalate.instrument/net.tascalate.instrument.parent/1.1.1/jar) [![GitHub release](https://img.shields.io/github/release/vsilaev/tascalate-instrument.svg)](https://github.com/vsilaev/tascalate-instrument/releases/tag/1.1.1) [![license](https://img.shields.io/github/license/vsilaev/tascalate-instrument.svg)](https://github.com/vsilaev/tascalate-instrument/blob/master/LICENSE)
# Tascalate Instrument
Utility classes to develop / use Java Agents across different Java versions (1.6 - 17+) - uniformly define classes in agent, attach agents dynamically, etc.

Currently there are two functional areas covered by the library: **attach**-ing Java Agents dynamically and **emit**-ting bytecode dynamically. Let's review them in alphabetical order.

# Instrument.Attach - dynamically attaching Java Agents.
Java platform provides several options to load Java Agents
1. You can package Java Agent's code to the JAR archive, add `Premain-Class: com.mycompany.myproject.MyAgentClass` manifest attribute, and supply `-javaagent:jarpath[=options]` parameter to the Java command line. Java runtime will invoke `MyAgentClass.premain(String agentArgs, Instrumentation inst)` of your agent class and can setup instrumentation.
2. Starting with Java 9, you may add `Launcher-Agent-Class: com.mycompany.myproject.MyAgentClass` to the main application JAR module. The JVM will invoke `MyAgentClass.agentmain(String agentArgs, Instrumentation inst)` before starting application's main method. Obviously, this option is only available when you have control on the main application - and this is not the case, say, when you have to add agent to the JEE server.
3. Starting with [Java 6](https://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html) there is also an option to load agents dynamically via specifying `Agent-Class: com.mycompany.myproject.MyAgentClass` manifest attribute in the agent's JAR file. However, the specification is pretty silent how to _attach_ this agent JAR on the fly.

There are several libraries that uses semi-documented and non-official way to implement option [3] - via using class `com.sun.tools.attach.VirtualMachine`. In JDK 9+ this class exists in module `jdk.attach`, in previous Java versions (1.6-1.8) it's located in `tools.jar` and it's location could be different depending on the operating system JDK runs on. Plus, IBM JVM's uses own naming (or 'branding') for this class for Java versions below 9. As you see, there is a lot of things that should be taken on account to implement `attach` functionality in a portable manner (across several JVM versions / vendors).

To make a matter worse, Oracle decided to restrict self-attaching Java Agents in Java 9. I.e. the application may not attach an agent to itself by default, but it can be done from an external Java application! Frankly, I can't find any logical explanations for this limitation. And don't even start with the security argument -- attaching an Agent to the VM from an external process requires the same amount of permissions (if not more) than self-attaching. So Oracle knows why, but since Java 9 there is a `jdk.attach.allowAttachSelf` system property, that is set by default to `false`! Please read [here](https://www.oracle.com/technetwork/java/javase/9-notes-3745703.html) for details.

All in all, Tascalate Instrument Attach provides a way to attach Java Agents dynamically to the running application in portable manner via "misuse of the Attach API", as Oracle calls it. To use a library you have to add the following Maven dependency:
```xml
<dependency>
    <groupId>net.tascalate.instrument</groupId>
    <artifactId>net.tascalate.instrument.attach</artifactId>
    <version>#actual-version#</version>
</dependency>
```
The library is a multi-release JAR that works with Java 1.6 - 17.
Next, in your code you are able to do the following:
```
import net.tascalate.instrument.attach.api.AgentLoader;
import net.tascalate.instrument.attach.api.AgentLoaders;
...
AgentLoader loader = AgentLoaders.getDefault();
loader.attach("./javaflow.instrument-continuations.jar");
loader.attach("./javaflow.instrument-cdi.jar", "skip-retransform");
```
or in the simplified form (if you need to load just a single agent):
```
import net.tascalate.instrument.attach.api.AgentLoaders;
...
AgentLoaders.attach("./javaflow.instrument-continuations.jar", null);
```
Both methods takes a path to the Java Agent JAR archive as a first parameter and an optional agent's arguments as a second parameter.
For Java 9+ applications it's necessary to add a dependency to the Tascalate Instrument Attach module:
```java
module net.tascalate.instrument.examples.app {
    requires net.tascalate.instrument.attach;
}
```
With this setup you will get working and portable code to attach Java Agents to the currently running application. But not the most optimal one, however. See, by default, for Java 9+ applications this code will create a small executable JAR to attach an agent to the current JVM from the external process and overcome self-attach restrictions. The better option is still possible, and it involves native calls via JNI - hence, it's not enabled by default. 

To add JNI (or actually, [JNA](https://github.com/java-native-access/jna)) attach, you have to change the following:
1. Add corresponding JNA dependecy:
```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.8.0</version>
</dependency>
```
2. Alter `module-info.java` of your application to add JNA module (obviously, this step is not necessary for Java 1.6-1.8 applications):
```java
module net.tascalate.instrument.examples.app {
    requires net.tascalate.instrument.attach;
    requires com.sun.jna;
}
```
No changes to the code that invokes `AgentLoaders.attach(...)` or `AgentLoaders.getDefault().attach(...)` are required! And the code may still be used with all supported Java versions (1.6 - 17+).

Footnote. I'm aware that similar functionality exists in several libraries, like [ByteBuddy](https://bytebuddy.net/). But it's either tied very hard to the library itself, or requires some extra dependencies, so I decided to roll-out my own solution with the only and _optional_ dependency to JNA. 

# Instrument.Emitter - defining Java classes dynamically
In the great article [JDK 11 and Proxies in a World Past sun.misc.Unsafe](https://dzone.com/articles/jdk-11-and-proxies-in-a-world-past-sunmiscunsafe) Rafael Winterhalter (the author of [ByteBuddy](https://bytebuddy.net/)) touches a serious issue with Java 11+: after removal of `sun.misc.Unsafe.defineClass` method there is no option left for developers of Java Agents to define classes dynamically in the _same_ class loader as the one used to load the _primary_ class. "Primary" in this case is a class that is the target of current (re)-transformation. 

To better understand the issue, let us recall how [ClassFileTransformer](https://docs.oracle.com/javase/9/docs/api/java/lang/instrument/ClassFileTransformer.html) interface is defined:
```
default byte[]	transform(ClassLoader loader, 
                          String className, 
                          Class<?> classBeingRedefined, 
                          ProtectionDomain protectionDomain, 
                          byte[] classfileBuffer)	

default byte[]	transform(Module module, 
                          ClassLoader loader, 
                          String className, 
                          Class<?> classBeingRedefined, 
                          ProtectionDomain protectionDomain, 
                          byte[] classfileBuffer)
```
The first method is available for Java 1.5-1.8, the second one is supported by Java version 9 and above. The only difference between them is that the modern version get a [Module](https://docs.oracle.com/javase/9/docs/api/java/lang/Module.html) as a parameter. But what important here, is that neither of them receives a `Class<?>` as a parameter, because `Class<?>` is not available at this phase (transformation). And it's even an error to call `ClassLoader.load(className)` here!

So if there is no class, then there is no way to get a [MethodHadles.Lookup](https://docs.oracle.com/javase/9/docs/api/java/lang/invoke/MethodHandles.Lookup.html) to execute later [MethodHandles.Lookup.defineClass](https://docs.oracle.com/javase/9/docs/api/java/lang/invoke/MethodHandles.Lookup.html#defineClass-byte:A-) to dynamically define any additional classes on the same class loader! Previously it was possible either via "deep reflection" on `ClassLoader` passed or via using `sun.misc.Unsafe.defineClass` with the very same class loader. No longer! By default, `java.lang` package is not opened to named modules since Java 9 and `sun.misc.Unsafe.defineClass` method is removed since Java 11. So any developer, who have to create additional classes during transformation of the `primary` class is blocked. Me included - my library [Tascalate Async/Await](https://github.com/vsilaev/tascalate-async-await) does exactly this: a body of the every asynchronous method of the trasformed classes is replaced with an anonymous class (the subclass of `Runnable`). And it's necessary to define all these anonymous classes within the very same class loader as the original class. I'm stucked. Here is why Tascalate Instrument library was started.

Notice, that in Java 9 we get a [Module](https://docs.oracle.com/javase/9/docs/api/java/lang/Module.html) as a first parameter? By the way, it's an [AnnotatedElement](https://docs.oracle.com/javase/9/docs/api/java/lang/reflect/AnnotatedElement.html) and it's possible to define annotations on it... It's getting warmer... And an annotation may have a class as an attribute:
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface MyModuleAnnotation {
    Class<?>[] value();
}
```
Now it's hot enough to see a solution! Unfortunately, this will put inevitable burden on library user, but there is no better way... anyway... So, the solution provided by Tascalate Instrument.Emitter library is:
1. The library provides the annotation `@AllowDynamicClasses` with `ElementType.MODULE` as a target. 
2. The `value` attribute of the annotation is defined as an array of classes, each extending `AbstractOpenPackage` class
3. Library user must place a subclass of `AbstractOpenPackage` superclass in _every_ package where dynamic classes should be defined. The class may be final, it may be abstract, but it must be public. See next step.
4. Library user must use the following definition inside `module-info`:
```java
import net.tascalate.instrument.emitter.api.AllowDynamicClasses;

import net.tascalate.instrument.examples.app.controllers.OpenPackageControllers;
import net.tascalate.instrument.examples.app.services.OpenPackageServices;
import net.tascalate.instrument.examples.app.transformers.OpenPackageTransformers;

@AllowDynamicClasses({OpenPackageControllers.class, OpenPackageServices.class, OpenPackageTransformers.class})
module net.tascalate.instrument.examples.app {
    requires net.tascalate.instrument.emitter;

    opens net.tascalate.instrument.examples.app.controllers 
       to net.tascalate.instrument.emitter;
       
    opens net.tascalate.instrument.examples.app.services 
       to net.tascalate.instrument.emitter;
       
    opens net.tascalate.instrument.examples.app.transformers 
       to net.tascalate.instrument.emitter;       
}
```
I.e. annotate the module with `@AllowDynamicClasses` and list all subclasses of `AbstractOpenPackage` as the value; additionally, open every target package to at least `net.tascalate.instrument.emitter` module. It's mandatory to use `requires net.tascalate.instrument.emitter` here while we are using its classes already. The library user completed her work to support defining classes dynamically. 

Now let us see what Java Agent developer should do. Remember, that we have 2 different versions of the `ClassTransformer.transform` method? So agent developer should do the following, depending on the target Java version (pre-9 or post-9):
```java
import net.tascalate.instrument.emitter.spi.ClassEmitter;
import net.tascalate.instrument.emitter.spi.ClassEmitterException;
import net.tascalate.instrument.emitter.spi.ClassEmitters;
...
ClassEmitter emitter = ClassEmitters.of(null, classLoader); // Java 1.6-1.8
ClassEmitter emitter = ClassEmitters.of(module, classLoader); // Java 9+
```
Obviously, the `module-info` of Java agent must include `requires net.tascalate.instrument.emitter`. Most probably, Java Agent developer will create multi-release JAR with two versions, each differs in a way it constructs `ClassEmiter`. 

To simplify this task, the library includes abstract PortableClassFileTransformer class that already provides construction of the necessary `ClassEmitter`. The Java Agent developer must extend it and implement the single abstract method:
```java
import net.tascalate.instrument.emitter.spi.ClassEmitter;
import net.tascalate.instrument.emitter.spi.ClassEmitterException;
import net.tascalate.instrument.emitter.spi.ClassEmitters;
import net.tascalate.instrument.emitter.spi.PortableClassFileTransformer;

public class MyClassTransformer extends PortableClassFileTransformer {
    @Override
    public byte[] transform(ClassEmitter emitter,
                            Object module,
                            ClassLoader loader,
                            String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, 
                            byte[] classfileBuffer) throws IllegalClassFormatException {
                            
        // Define classes using ClassEmitter emitter 
    }
}
```

After this everything will be uniform across all Java versions:
```java
ClassEmitter emitter = ...; // created as above or get via parameter in PortableClassFileTransformer
emitter.defineClass(classfileBuffer, protectionDomain);
```
You see that the rest of the code is identical and agnostic to the Java version used. All the burden to support "modular class definitions" is put to the end-user of the library. And to the Tascalate Instrument.Emitter library itself, sure.

Additionally, Tascalate Instrument.Emitter provides a portable way to define classes not only to Java Agent developers, but to the authors of general bytecode modifications libraies, like [ByteBuddy](https://bytebuddy.net/) or [CGLib](https://github.com/cglib/cglib). If you check `ClassEmitters` sources, pay attention to the name of the first parameter:
```java
public static ClassEmitter of(Object moduleOrClass, ClassLoader classLoader)
```
Yes, it accepts either `Class<?>` or `Module`. Or should be null otherwise. Depending on the currently running Java version (and other conditions, like named vs unnamed modules, JVM args), the method will return `ClassEmitter` that depends either on `Module` (if class is from `@AllowDynamicClasses` module) or for the `ClassLoader` of the class. So the author of the bytecode modification library is isolated from the specifics of Java, and may use portable `ClassEmitters.of(someSuperClass, someSuperClass.getClassLoader())` calls inside own code. And the end-user will adopt the module as necessary and when necessary.

It worth to mention, that developers, who creates own custom class loaders, may implement `ClassEmitter` interface for the custom class loader. And this emitter will take precedence in the emitter-resolution algorithm.

To use the library, you have to add the single dependency:
```xml
<dependency>
    <groupId>net.tascalate.instrument</groupId>
    <artifactId>net.tascalate.instrument.emitter</artifactId>
    <version>#actual-version#</version>
</dependency>
```
