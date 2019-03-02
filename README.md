# Tascalate Instrument
Utility classes to develop / use Java Agents across different Java versions (1.6 - 11+) - uniformly define classes in agent, attach agents dynamically, etc.

Currently there are two functional areas covered by the library: **attach**-ing Java Agents dynamically and **emit**-ting bytecode dynamically. Let's review them in alphabetical order.

# Instrument.Attach - dynamically attaching Java Agents.
Java platform provides several options to load Java Agents
1. You can package Java Agent's code to the JAR archive, add `Premain-Class: com.mycompany.myproject.MyAgentClass` manifest attribute, and supply `-javaagent:jarpath[=options]` parameter to the Java command line. Java runtime will invoke `MyAgentClass.premain(String agentArgs, Instrumentation inst)` of your agent class and can setup instrumentation.
2. Starting with Java 9, you may add `Launcher-Agent-Class: com.mycompany.myproject.MyAgentClass` to the main application JAR module. The JVM will invoke `MyAgentClass.agentmain(String agentArgs, Instrumentation inst)` before starting application's main method. Obviously, this option is only available when you have control on the main application - and this is not the case, say, when you have to add agent to the JEE server.
3. Starting with [Java 6](https://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html) there is also an option to load agents dynamically via specifying `Agent-Class: com.mycompany.myproject.MyAgentClass` manifest attribute in the agent's JAR file. However, the specification is pretty silent how to _attach_ this agent JAR on the fly.

There are several libraries that uses semi-documented and non-official way to implement option [3] - via using class `com.sun.tools.attach.VirtualMachine`. In JDK 9+ this class exists in module `jdk.attach`, in previous Java versions (1.6-1.8) it's located in `tools.jar`. Plus, IBM JVM's uses own naming (or 'branding') for this class for Java versions below 9. All in all, there is a lot of things that should be taken on account to implement `attach` functionality in a portable manner (across several JVM versions / vendors).

To make a matter worse, Oracle deceided to restrict self-attaching Java Agents in Java 9. I.e. the application may not attach agent to itself by default, but it can be done from an external Java application! Frankly, I can't find any logical explanations for this limitation. And don't even start with the security argument -- attaching an Agent to the VM from an external process requires the same amount of permissions (if not more) than self-attaching. So Oracle knows why, but since Java 9 there is a `jdk.attach.allowAttachSelf` system property, that is set by default to `false`! Please read [here](https://www.oracle.com/technetwork/java/javase/9-notes-3745703.html) for details.

All in all, Tascalate Instrument Attach provides a way to attach Java Agents dynamically to the running application in portable manner via "misuse of the Attach API", as Oracle calls it. To use a library you have to add the following Maven dependency:
```xml
<dependency>
    <groupId>net.tascalate.instrument</groupId>
    <artifactId>net.tascalate.instrument.attach</artifactId>
    <version>#actual-version#</version>
</dependency>
```
The library is a multi-release JAR that works with Java 1.6 - 11.
Next, in your code you have to invoke the following:
```
import net.tascalate.instrument.attach.api.AgentLoader;
import net.tascalate.instrument.attach.api.AgentLoaders;
...
AgentLoader loader = AgentLoaders.getDefault();
loader.attach("./javaflow.instrument-continuations.jar");
loader.attach("./javaflow.instrument-cdi.jar", "skip-retranfrom");
```
or in simplified form (if you need to load just a single agent):
```
import net.tascalate.instrument.attach.api.AgentLoaders;
...
AgentLoaders.attach("./javaflow.instrument-continuations.jar", null);
```
Both methods takes a path to Java Agent JAR archive as a first parameter and an optional agent's arguments as a second parameter.
For Java 9+ applications it's necessary to add a dependency to Tascalate Instrument Attach module:
```java
module net.tascalate.instrument.examples.app {
    requires net.tascalate.instrument.attach;
}
```
With this setup you will get working and portable code to attach Java Agents to the currently running application. But not the most optimal one, however. See, by default, for Java 9+ applications this code will create a small executable JAR to attach an agent to the current JVM from external process and overcome self-attach restrictions. The better option is still possible, and it involves native JNI calls - hence, it's not enabled by default. 

To add JNI (or actually, [JNA](https://github.com/java-native-access/jna)) attach, you have to change the following:
1. Add corresponding JNA dependecy:
```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.2.0</version>
</dependency>
```
2. Alter `module-info.java` of your application to add JNA module (obviously, this step is not necessary for Java 1.6-1.8 applications):
```java
module net.tascalate.instrument.examples.app {
    requires net.tascalate.instrument.attach;
    requires com.sun.jna;
}
```
No changes to the code that invokes `AgentLoaders.attach(...)` or `AgentLoaders.getDefault().attach(...)` are required! And the code may still be used with all supported Java versions (1.6 - 11+).

Disclaimer. I'm aware that similar functionality exists in several libraries, like [ByteBuddy](https://bytebuddy.net/). But it's either tied very hard to the library itself, or requires some extra dependencies, so I deceided to roll-out my own solution with the only and _optional_ dependency to JNA. 

# Instrument.Emit - defining Java classes dynamically
