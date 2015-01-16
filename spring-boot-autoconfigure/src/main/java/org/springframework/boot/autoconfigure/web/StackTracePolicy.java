package org.springframework.boot.autoconfigure.web;

/**
 * An Enumeration of stacktrace policies for the {@link ErrorController} to
 * determine whether to add stacktrace information to the model or not
 *
 * @author Michael Stummvoll
 */
public enum StackTracePolicy {
	/**
	 * Always add stacktrace information to the model
	 */
	ALWAYS,

	/**
	 * Never add stacktrace information to the model
	 */
	NEVER,

	/**
	 * Add stacktrace information to the model depending on the "trace" request
	 * parameter
	 */
	REQUEST
}
