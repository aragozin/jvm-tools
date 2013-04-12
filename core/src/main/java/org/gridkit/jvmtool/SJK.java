package org.gridkit.jvmtool;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.coherence.cachecli.CacheCli.Cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Swiss Java Knife
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 * 
 */
public class SJK {

	public static void fail(String... messages) {
		throw new CommandAbortedError(messages);
	}

	@Parameter(names = "--help", help = true)
	private boolean help = false;

	private boolean suppressSystemExit;

	private Map<String, Runnable> commands = new HashMap<String, Runnable>();

	public void logError(String line) {
		System.err.println(line);
	}

	public boolean start(String[] args) {
		try {

			JCommander parser = new JCommander(this);
			
			addCommands(parser);
			
			try {
				parser.parse(args);
			}
			catch(Exception e) {
				logError(e.toString());
				parser.usage();
				fail();
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
			else {
				
				Runnable cmd = commands.get(parser.getParsedCommand());
					
				if (cmd == null) {
					parser.usage();
					fail();
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

	private void addCommands(JCommander parser) {
		
		
	}

	@SuppressWarnings("serial")
	public static class CommandAbortedError extends Error {

		public String[] messages;

		public CommandAbortedError(String[] messages) {
			super();
			this.messages = messages;
		}

	}

}
