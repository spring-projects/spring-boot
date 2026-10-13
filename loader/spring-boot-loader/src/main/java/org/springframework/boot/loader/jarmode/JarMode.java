/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jarmode;

/**
 * Interface registered in {@code spring.factories} to provides extended 'jarmode'
 * support.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface JarMode {

	/**
	 * Returns if this accepts and can run the given mode.
	 * @param mode the mode to check
	 * @return if this instance accepts the mode
	 */
	boolean accepts(String mode);

	/**
	 * Run the jar in the given mode.
	 * @param mode the mode to use
	 * @param args any program arguments
	 * @throws JarModeErrorException on an error that should print a simple error message
	 */
	void run(String mode, String[] args) throws JarModeErrorException;

}
