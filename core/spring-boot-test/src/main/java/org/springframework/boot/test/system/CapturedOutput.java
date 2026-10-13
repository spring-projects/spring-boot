/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.system;

/**
 * Provides access to {@link System#out System.out} and {@link System#err System.err}
 * output that has been captured by the {@link OutputCaptureExtension} or
 * {@link OutputCaptureRule}. Can be used to apply assertions either using AssertJ or
 * standard JUnit assertions. For example: <pre class="code">
 * assertThat(output).contains("started"); // Checks all output
 * assertThat(output.getErr()).contains("failed"); // Only checks System.err
 * assertThat(output.getOut()).contains("ok"); // Only checks System.out
 * </pre>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.2.0
 * @see OutputCaptureExtension
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
