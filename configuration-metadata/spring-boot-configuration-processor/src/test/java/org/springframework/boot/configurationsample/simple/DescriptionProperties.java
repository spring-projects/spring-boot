/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties with various description styles.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("description")
public class DescriptionProperties {

	/**
	 * A simple description.
	 */
	private String simple;

	/**
	 * This is a lengthy description that spans across multiple lines to showcase that the
	 * line separators are cleaned automatically.
	 */
	private String multiLine;

	// @formatter:off
	/**
	 * This is an example of a description
	 *with unusual whitespace after a new line.
	 */
	private String multiLineWhitespace;
	// @formatter:on

	public String getSimple() {
		return this.simple;
	}

	public void setSimple(String simple) {
		this.simple = simple;
	}

	public String getMultiLine() {
		return this.multiLine;
	}

	public void setMultiLine(String multiLine) {
		this.multiLine = multiLine;
	}

	public String getMultiLineWhitespace() {
		return this.multiLineWhitespace;
	}

	public void setMultiLineWhitespace(String multiLineWhitespace) {
		this.multiLineWhitespace = multiLineWhitespace;
	}

}
