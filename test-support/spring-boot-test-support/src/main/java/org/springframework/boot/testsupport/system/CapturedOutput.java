/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.system;

/**
 * Internal test class providing access to {@link System#out System.out} and
 * {@link System#err System.err} output that has been captured.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public interface CapturedOutput extends CharSequence {

	@Override
	default int length() {
		return toString().length();
	}

	@Override
	default char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	default CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	/**
	 * Return all content (both {@link System#out System.out} and {@link System#err
	 * System.err}) in the order that it was captured.
	 * @return all captured output
	 */
	String getAll();

	/**
	 * Return {@link System#out System.out} content in the order that it was captured.
	 * @return {@link System#out System.out} captured output
	 */
	String getOut();

	/**
	 * Return {@link System#err System.err} content in the order that it was captured.
	 * @return {@link System#err System.err} captured output
	 */
	String getErr();

}
