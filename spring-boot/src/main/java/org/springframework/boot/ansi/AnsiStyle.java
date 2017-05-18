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
 * {@link AnsiElement Ansi} styles.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public enum AnsiStyle implements AnsiElement {

	NORMAL("0"),

	BOLD("1"),

	FAINT("2"),

	ITALIC("3"),

	UNDERLINE("4");

	private final String code;

	AnsiStyle(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return this.code;
	}

}
