/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.cli;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Swiss Java Knife
 * <br/>
 * Command line tool for JVM troubleshooting
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CommandLauncher {

	public interface CmdRef {
				
		public String getCommandName();

		public Runnable newCommand(CommandLauncher host);
		
	}
	
	@Parameter(names = "--help", help = true)
	private boolean help = false;

	@Parameter(names = {"-X", "--verbose"}, description = "Enable detailed diagnostics")
	private boolean verbose = false;

	@Parameter(names = "--commands", help = true)
	private boolean listCommands = false;
	
	private boolean suppressSystemExit;

	private Map<String, Runnable> commands = new HashMap<String, Runnable>();

	public boolean isVerbose() {
	    return verbose;
	}
	
    public Error fail(String... messages) {
        throw new CommandAbortedError(false, messages);
    }

    public Error fail(String message, Exception e) {
        throw new CommandAbortedError(false, new String[]{message}, e);
    }

    public Error failAndPrintUsage(String... messages) {
        throw new CommandAbortedError(true, messages);
    }
	
	public void logError(String line) {
		System.err.println(line);
	}

	public void logTrace(Throwable e) {
	    e.printStackTrace(System.err);
	}

	public void suppressSystemExit() {
		suppressSystemExit = true;
	}
	
	public boolean start(String[] args) {
		breakCage(args);
		JCommander parser = null;
		try {

			parser = new JCommander(this);
			
			addCommands(parser);
			
			try {
				parser.parse(args);
			}
			catch(Exception e) {
				failAndPrintUsage(e.toString());
			}

			if (help) {
				String cmd = parser.getParsedCommand();
				if (cmd == null) { 
					parser.usage();
				}
				else {
					parser.usage(cmd);
				}							
			}
			else if (listCommands) {
			    for(String cmd: commands.keySet()) {
			        System.out.println(String.format("%8s - %s", cmd, parser.getCommandDescription(cmd)));
			    }
			}
			else {
				
				Runnable cmd = commands.get(parser.getParsedCommand());
					
				if (cmd == null) {
					failAndPrintUsage();
				}
				else {
					cmd.run();
				}				
			}			
			
			if (suppressSystemExit) {
				return true;
			}
			else {
				System.exit(0);
			}
		}
		catch(CommandAbortedError error) {
			for(String m: error.messages) {
				logError(m);
			}
			if (verbose && error.getCause() != null) {
			    logTrace(error.getCause());
			}
			if (error.printUsage && parser != null) {
				if (parser.getParsedCommand() != null) {
					parser.usage(parser.getParsedCommand());
				}
				else {
					parser.usage();
				}
			}
		}
		catch(Throwable e) {
			e.printStackTrace();
		}

		// abnormal termination
		if (suppressSystemExit) {
			return false;
		}
		else {
			System.exit(1);
			return false;
		}		
	}

	protected String[] getModulesUnlockCommand() {
		return new String[0];
	}
	
	protected List<String> getCommandPackages() {
	    return Collections.singletonList(getClass().getPackage().getName() + ".cmd");
	}
	
	private void addCommands(JCommander parser) throws InstantiationException, IllegalAccessException {
	    for(String pack: getCommandPackages()) {
    		for(Class<?> c: findClasses(pack)) {
    			if (CmdRef.class.isAssignableFrom(c)) {
    				CmdRef cmd = (CmdRef) c.newInstance();
    				String cmdName = cmd.getCommandName();
    				Runnable cmdTask = cmd.newCommand(this);
    				if (commands.containsKey(cmdName)) {
    					fail("Ambiguous implementation for '" + cmdName + "'");
    				}
    				commands.put(cmdName, cmdTask);
    				parser.addCommand(cmdName, cmdTask);
    			}
    		}
	    }
	}

	private List<Class<?>> findClasses(String packageName) {
		List<Class<?>> result = new ArrayList<Class<?>>();
		try {
			String path = packageName.replace('.', '/');
			for(String f: findFiles(path)) {
				if (f.endsWith(".class") && f.indexOf('$') < 0) {
					f = f.substring(0, f.length() - ".class".length());
					f = f.replace('/', '.');
					result.add(Class.forName(f));
				}
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	static List<String> findFiles(String path) throws IOException {
		List<String> result = new ArrayList<String>();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> en = cl.getResources(path);
		while(en.hasMoreElements()) {
			URL u = en.nextElement();
			listFiles(result, u, path);
		}
		return result;
	}
	
	static List<String> listFiles(List<String> results, URL packageURL, String path) throws IOException {

	    if(packageURL.getProtocol().equals("jar")){
	        String jarFileName;
	        JarFile jf ;
	        Enumeration<JarEntry> jarEntries;
	        String entryName;

	        // build jar file name, then loop through zipped entries
	        jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
	        jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
	        jf = new JarFile(jarFileName);
	        jarEntries = jf.entries();
	        while(jarEntries.hasMoreElements()){
	            entryName = jarEntries.nextElement().getName();
	            if(entryName.startsWith(path)){
	                results.add(entryName);
	            }
	        }

	    // loop through files in classpath
	    }else{
	        File dir = new File(packageURL.getFile());
	        String cp = dir.getCanonicalPath();
	        File root = dir;
	        while(true) {
	        	if (cp.equals(new File(root, path).getCanonicalPath())) {
	        		break;
	        	}
	        	root = root.getParentFile();
	        }
	        listFiles(results, root, dir);
	    }
	    return results;
	}

	static void listFiles(List<String> names, File root, File dir) {
		String rootPath = root.getAbsolutePath(); 
		if (dir.exists() && dir.isDirectory()) {
			for(File file: dir.listFiles()) {
				if (file.isDirectory()) {
					listFiles(names, root, file);
				}
				else {
					String name = file.getAbsolutePath().substring(rootPath.length() + 1);
					name = name.replace('\\', '/');
					names.add(name);
				}
			}
		}
	}	
	
	
	@SuppressWarnings("serial")
	public static class CommandAbortedError extends Error {

		public boolean printUsage;
		public String[] messages;

		public CommandAbortedError(boolean printUsage, String[] messages) {
			super();
			this.printUsage = printUsage;
			this.messages = messages;
		}

		public CommandAbortedError(boolean printUsage, String[] messages, Exception e) {
		    super(e);
		    this.printUsage = printUsage;
		    this.messages = messages;
		}
	}
	
	// special hack to workaround Java module system
	private void breakCage(String... args) {
		if ("false".equalsIgnoreCase(System.getProperty("sjk.breakCage", "true"))) {
			// do not break
			return;
		}
		RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
		String spec = rtBean.getSpecVersion();
		if (spec.startsWith("1.")) {
			// good classic Java
			return;
		}
		else {
			if (getModulesUnlockCommand().length > 0) {
				// we need to unlock some modules
				StringBuilder sb = new StringBuilder();
				for(String a: rtBean.getInputArguments()) {
					if (sb.length() > 0) {
						sb.append(" ");
					}
					sb.append(a);
				}
				if (isUnlocked(sb.toString(), getModulesUnlockCommand())) {
					// modules are unlocked
					return;
				}
				else {
					// break cage
					List<String> command = new ArrayList<String>();
					File jhome = new File(System.getProperty("java.home"));
					File jbin = new File(jhome, "bin/java");
					command.add(jbin.getPath());
					for(String m: getModulesUnlockCommand()) {
						command.add("--add-exports");
						command.add(m);
					}
					command.add("-Dsjk.breakCage=false");
					command.add("-cp");
					command.add(rtBean.getClassPath());
					command.addAll(rtBean.getInputArguments());
					command.add(this.getClass().getName());
					command.addAll(Arrays.asList(args));
					
					System.err.println("Restarting java with unlocked package access");
					
					ProcessSpawner.start(command);
				}
			}
		}
	}

	private boolean isUnlocked(String cmd, String[] modules) {
		for(String m: modules) {
			String c1 = "--add-exports " + m;
			String c2 = "--add-exports=" + m;
			if (!cmd.contains(c1) && !cmd.contains(c2)) {
				return false;
			}
		}
		return true;
	}
}
