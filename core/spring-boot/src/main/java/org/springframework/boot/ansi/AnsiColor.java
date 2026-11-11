/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ansi;

/**
 * {@link AnsiElement Ansi} colors.
 *
 * @author Phillip Webb
 * @author Geoffrey Chandler
 * @since 1.3.0
 */
public enum AnsiColor implements AnsiElement {

	DEFAULT("39"),

	BLACK("30"),

	RED("31"),

	GREEN("32"),

	YELLOW("33"),

	BLUE("34"),

	MAGENTA("35"),

	CYAN("36"),

	WHITE("37"),

	BRIGHT_BLACK("90"),

	BRIGHT_RED("91"),

	BRIGHT_GREEN("92"),

	BRIGHT_YELLOW("93"),

	BRIGHT_BLUE("94"),

	BRIGHT_MAGENTA("95"),

	BRIGHT_CYAN("96"),

	BRIGHT_WHITE("97");

	private final String code;

	AnsiColor(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return this.code;
	}

}
