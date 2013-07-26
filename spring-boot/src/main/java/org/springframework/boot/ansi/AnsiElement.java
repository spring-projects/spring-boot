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

	public static final AnsiElement BLACK = new DefaultAnsiElement("30");
	public static final AnsiElement RED = new DefaultAnsiElement("31");
	public static final AnsiElement GREEN = new DefaultAnsiElement("32");
	public static final AnsiElement YELLOW = new DefaultAnsiElement("33");
	public static final AnsiElement BLUE = new DefaultAnsiElement("34");
	public static final AnsiElement MAGENTA = new DefaultAnsiElement("35");
	public static final AnsiElement CYAN = new DefaultAnsiElement("36");
	public static final AnsiElement WHITE = new DefaultAnsiElement("37");
	public static final AnsiElement DEFAULT = new DefaultAnsiElement("39");

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
