package com.sun.tools.attach;

/**
 * Compile time only mock for attach API.
 */
public abstract class AttachPermission extends java.security.BasicPermission {

	private static final long serialVersionUID = 20120402L;

	protected AttachPermission(String name, String actions) {
		super(name, actions);
	}

	protected AttachPermission(String name) {
		super(name);
	}
}
