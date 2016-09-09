/*
 * Copyright 2012-2015 the original author or authors.
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

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiStyle#NORMAL}
	 */
	@Deprecated
	AnsiElement NORMAL = new DefaultAnsiElement("0");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiStyle#BOLD}
	 */
	@Deprecated
	AnsiElement BOLD = new DefaultAnsiElement("1");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiStyle#FAINT}
	 */
	@Deprecated
	AnsiElement FAINT = new DefaultAnsiElement("2");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiStyle#ITALIC}
	 */
	@Deprecated
	AnsiElement ITALIC = new DefaultAnsiElement("3");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiStyle#UNDERLINE}
	 */
	@Deprecated
	AnsiElement UNDERLINE = new DefaultAnsiElement("4");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#BLACK}
	 */
	@Deprecated
	AnsiElement BLACK = new DefaultAnsiElement("30");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#RED}
	 */
	@Deprecated
	AnsiElement RED = new DefaultAnsiElement("31");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#GREEN}
	 */
	@Deprecated
	AnsiElement GREEN = new DefaultAnsiElement("32");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#YELLOW}
	 */
	@Deprecated
	AnsiElement YELLOW = new DefaultAnsiElement("33");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#BLUE}
	 */
	@Deprecated
	AnsiElement BLUE = new DefaultAnsiElement("34");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#MAGENTA}
	 */
	@Deprecated
	AnsiElement MAGENTA = new DefaultAnsiElement("35");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#CYAN}
	 */
	@Deprecated
	AnsiElement CYAN = new DefaultAnsiElement("36");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#WHITE}
	 */
	@Deprecated
	AnsiElement WHITE = new DefaultAnsiElement("37");

	/**
	 * @deprecated in 1.3.0 in favor of {@link AnsiColor#DEFAULT}
	 */
	@Deprecated
	AnsiElement DEFAULT = new DefaultAnsiElement("39");

	/**
	 * @return the ANSI escape code
	 */
	@Override
	String toString();

	/**
	 * Internal default {@link AnsiElement} implementation.
	 */
	class DefaultAnsiElement implements AnsiElement {

		private final String code;

		DefaultAnsiElement(String code) {
			this.code = code;
		}

		@Override
		public String toString() {
			return this.code;
		}

	}

}
