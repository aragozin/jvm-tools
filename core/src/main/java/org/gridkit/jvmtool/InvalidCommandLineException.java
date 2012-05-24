package org.gridkit.jvmtool;

public class InvalidCommandLineException extends Exception {

	private static final long serialVersionUID = 20120524L;

	public InvalidCommandLineException() {
		super();
	}

	public InvalidCommandLineException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidCommandLineException(String message) {
		super(message);
	}

	public InvalidCommandLineException(Throwable cause) {
		super(cause);
	}
}
