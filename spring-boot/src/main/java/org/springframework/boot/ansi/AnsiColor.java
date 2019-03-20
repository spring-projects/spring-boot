/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
