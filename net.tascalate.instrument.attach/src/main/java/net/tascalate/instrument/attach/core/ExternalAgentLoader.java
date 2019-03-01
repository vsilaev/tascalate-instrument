package net.tascalate.instrument.attach.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import net.tascalate.instrument.attach.api.AgentLoader;
import net.tascalate.instrument.attach.api.AgentLoaderException;

public class ExternalAgentLoader extends AbstractAgentLoader implements SafeAgentLoader {

	private final File alternativeToolsJar;
	
	public ExternalAgentLoader() {
		this(null);
	}
	
	public ExternalAgentLoader(File alternativeToolsJar) {
		this.alternativeToolsJar = alternativeToolsJar;
	}
	
	@Override
	public boolean isAvailable() {
	    return new LocalAgentLoader(alternativeToolsJar).isExternalAttachPossible();
	}
	
	@Override
	public void attach(String agentJarPath, String agentParams) {
		attach(agentJarPath, agentParams, CurrentProcess.pid());
	}
	
	void attach(String agentJarPath, String agentParams, long pid) {
		try {
		    Class<?>[] classes = {
		        AgentLoader.class, 
		        AgentLoaderException.class,
		        AbstractAgentLoader.class,
		        SafeAgentLoader.class,
		        ExternalAgentLoader.class, 
		        CurrentProcess.class		            
		    };
			File runnableJar = createJarFile(new HashSet<Class<?>>(Arrays.asList(classes)), LocalAgentLoader.class);

			System.out.println("Using temporary executable JAR: " + runnableJar.getCanonicalPath());
			try {
				File currentDirectory = new File(".").getCanonicalFile();
				String javaCommand = 
					System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + 
		            (System.getProperty("os.name", "").toLowerCase(Locale.US).contains("windows") ? "java.exe" : "java");
				
				String classPath = System.getProperty("java.class.path");
				if (null == classPath || classPath.length() == 0) {
				    classPath = "./";
				}
				
				List<String> fullCommand = new ArrayList<String>(
					Arrays.asList(
						escape(javaCommand),
                        "-cp",
                        escape(classPath),
						"-jar",
						escape(runnableJar.getCanonicalPath()),
						escape(agentJarPath),
		                String.valueOf( pid )
					)
				);
				if (null != agentParams && agentParams.length() > 0) {
					fullCommand.add(escape(agentParams));
				} else if (null != alternativeToolsJar) {
					fullCommand.add("--");
				}
				
				if (null != alternativeToolsJar) {
					fullCommand.add(escape(alternativeToolsJar.getCanonicalPath()));
				}
				
                System.out.println("Using Java command: " + fullCommand);

                ProcessBuilder processBuilder = new ProcessBuilder()
                    .directory(currentDirectory)
                    .command(fullCommand);

				Process process = redirected(processBuilder).start(); 
				
				int ret = process.waitFor();
				if (ret != 0) {
					throw new IllegalStateException("Could not self-attach to current VM using external process, exit code is: " + ret);
				} else {
					System.out.println("External attach process completed successfully");
				}
			} finally {
				runnableJar.delete();
			}
		} catch (InterruptedException ex) {
			throw new IllegalStateException("Could not self-attach to current VM using external process", ex);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not self-attach to current VM using external process", ex);
		}
	}
	
    @Override
    public String toString() {
        return getClass().getName() + "[load-method=fork-external-attach]";
    }
    
	private static File createJarFile(Set<Class<?>> classes, Class<?> mainClass) throws IOException {
        File agentJar = File.createTempFile("tal-ext", ".jar");
        agentJar.deleteOnExit();
        
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass.getName());
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(agentJar), manifest);
        Set<Class<?>> allClasses = new HashSet<Class<?>>(classes);
        allClasses.add(mainClass);
        try {
        	for (Class<?> clazz : allClasses) {
        		String resourceName = clazz.getName().replace('.', '/') + ".class";
                jarOutputStream.putNextEntry(new JarEntry(resourceName));
                byte[] buffer = new byte[8192];
                int size;
                InputStream inputStream = ExternalAgentLoader.class.getResourceAsStream('/' + resourceName);
                try {
	                while ((size = inputStream.read(buffer)) > 0) {
	                    jarOutputStream.write(buffer, 0, size);
	                }
	                jarOutputStream.closeEntry();
                } finally {
                	try {
                		inputStream.close();
                	} catch (IOException exIgnore) {
                		
                	}
                }
        	}
        } finally {
            jarOutputStream.close();
        }
        return agentJar;
    }
	
	private static ProcessBuilder redirected(ProcessBuilder processBuilder) {
	    if (null == REDIRECT_OUTPUT) {
	        return processBuilder;
	    } else {
	        try {
	            processBuilder = (ProcessBuilder)REDIRECT_OUTPUT.invoke(processBuilder, REDIRECT_INHERIT);
	            processBuilder = (ProcessBuilder)REDIRECT_ERROR.invoke(processBuilder, REDIRECT_INHERIT);
	        } catch (InvocationTargetException ex) {
	            throw new RuntimeException(ex);
	        } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
	        return processBuilder;
	    }
	}
	
    private static String escape(String value) {
        return value.contains(" ") ? '"' + value + '"' : value;
    }
    
    private static final Method REDIRECT_OUTPUT;
    private static final Method REDIRECT_ERROR;
    private static final Object REDIRECT_INHERIT;
    
    static {
        Class<?> processBuilderClass = ProcessBuilder.class;
        Method redirectOutput  = null;
        Method redirectError   = null;
        Object redirectInherit = null;
        try {
            Class<?> redirectClass = Class.forName("java.lang.ProcessBuilder$Redirect");
            
            Field redirectInheritFiled = redirectClass.getField("INHERIT");
            redirectInherit = redirectInheritFiled.get(null);
            
            redirectOutput = processBuilderClass.getMethod("redirectOutput", redirectClass);
            redirectError  = processBuilderClass.getMethod("redirectError",  redirectClass);
        } catch (ClassNotFoundException ex) {
            //Pitty
        } catch (NoSuchMethodException ex) {
            // Well, Java 7 rulezzz!!!
        } catch (NoSuchFieldException ex) {
            
        } catch (IllegalAccessException ex) {
            
        }   
        REDIRECT_OUTPUT  = redirectOutput;
        REDIRECT_ERROR   = redirectError;
        REDIRECT_INHERIT = redirectInherit;
    }
}
