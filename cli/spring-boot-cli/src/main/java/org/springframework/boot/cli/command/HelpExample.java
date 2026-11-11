/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command;

/**
 * An example that can be displayed in the help.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
public class HelpExample {

	private final String description;

	private final String example;

	/**
	 * Create a new {@link HelpExample} instance.
	 * @param description the description (in the form "to ....")
	 * @param example the example
	 */
	public HelpExample(String description, String example) {
		this.description = description;
		this.example = example;
	}

	public String getDescription() {
		return this.description;
	}

	public String getExample() {
		return this.example;
	}

}
