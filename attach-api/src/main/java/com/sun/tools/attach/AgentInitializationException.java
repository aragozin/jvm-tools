package com.sun.tools.attach;

/**
 * Compile time only mock for attach API.
 */
@SuppressWarnings("serial")
public abstract class AgentInitializationException extends Exception {

    public abstract int returnValue();
}
