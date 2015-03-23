/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.ansi;

/**
 * An ANSI encodable element.
 *
 * @author Phillip Webb
 */
public interface AnsiElement {

	public static final AnsiElement NORMAL = new DefaultAnsiElement("0");

	public static final AnsiElement BOLD = new DefaultAnsiElement("1");

	public static final AnsiElement FAINT = new DefaultAnsiElement("2");

	public static final AnsiElement ITALIC = new DefaultAnsiElement("3");

	public static final AnsiElement UNDERLINE = new DefaultAnsiElement("4");

	/*
	Normal foreground colors
	 */
	public static final AnsiElement BLACK = new DefaultAnsiElement("30");

	public static final AnsiElement RED = new DefaultAnsiElement("31");

	public static final AnsiElement GREEN = new DefaultAnsiElement("32");

	public static final AnsiElement YELLOW = new DefaultAnsiElement("33");

	public static final AnsiElement BLUE = new DefaultAnsiElement("34");

	public static final AnsiElement MAGENTA = new DefaultAnsiElement("35");

	public static final AnsiElement CYAN = new DefaultAnsiElement("36");

	public static final AnsiElement WHITE = new DefaultAnsiElement("37");

	public static final AnsiElement DEFAULT = new DefaultAnsiElement("39");

	/*
	High intensity foreground colors
	 */
	public static final AnsiElement BRIGHT_BLACK = new DefaultAnsiElement("90");

	public static final AnsiElement BRIGHT_RED = new DefaultAnsiElement("91");

	public static final AnsiElement BRIGHT_GREEN = new DefaultAnsiElement("92");

	public static final AnsiElement BRIGHT_YELLOW = new DefaultAnsiElement("93");

	public static final AnsiElement BRIGHT_BLUE = new DefaultAnsiElement("94");

	public static final AnsiElement BRIGHT_MAGENTA = new DefaultAnsiElement("95");

	public static final AnsiElement BRIGHT_CYAN = new DefaultAnsiElement("96");

	public static final AnsiElement BRIGHT_WHITE = new DefaultAnsiElement("97");

	/*
	Normal background colors
	 */
	public static final AnsiElement BG_BLACK = new DefaultAnsiElement("40");

	public static final AnsiElement BG_RED = new DefaultAnsiElement("41");

	public static final AnsiElement BG_GREEN = new DefaultAnsiElement("42");

	public static final AnsiElement BG_YELLOW = new DefaultAnsiElement("43");

	public static final AnsiElement BG_BLUE = new DefaultAnsiElement("44");

	public static final AnsiElement BG_MAGENTA = new DefaultAnsiElement("45");

	public static final AnsiElement BG_CYAN = new DefaultAnsiElement("46");

	public static final AnsiElement BG_WHITE = new DefaultAnsiElement("47");

	public static final AnsiElement BG_DEFAULT = new DefaultAnsiElement("49");

	/*
	High intensity background colors
	 */
	public static final AnsiElement BG_BRIGHT_BLACK = new DefaultAnsiElement("100");

	public static final AnsiElement BG_BRIGHT_RED = new DefaultAnsiElement("101");

	public static final AnsiElement BG_BRIGHT_GREEN = new DefaultAnsiElement("102");

	public static final AnsiElement BG_BRIGHT_YELLOW = new DefaultAnsiElement("103");

	public static final AnsiElement BG_BRIGHT_BLUE = new DefaultAnsiElement("104");

	public static final AnsiElement BG_BRIGHT_MAGENTA = new DefaultAnsiElement("105");

	public static final AnsiElement BG_BRIGHT_CYAN = new DefaultAnsiElement("106");

	public static final AnsiElement BG_BRIGHT_WHITE = new DefaultAnsiElement("107");


	/**
	 * @return the ANSI escape code
	 */
	@Override
	public String toString();

	/**
	 * Internal default {@link AnsiElement} implementation.
	 */
	static class DefaultAnsiElement implements AnsiElement {

		private final String code;

		public DefaultAnsiElement(String code) {
			this.code = code;
		}

		@Override
		public String toString() {
			return this.code;
		}

	}

}
