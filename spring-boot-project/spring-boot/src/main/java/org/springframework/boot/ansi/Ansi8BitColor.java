/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * {@link AnsiElement} implementation for ANSI 8-bit foreground or background color codes.
 *
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @since 2.2.0
 * @see #foreground(int)
 * @see #background(int)
 */
public final class Ansi8BitColor implements AnsiElement {

	private final String prefix;

	private final int code;

	/**
	 * Create a new {@link Ansi8BitColor} instance.
	 * @param prefix the prefix escape chars
	 * @param code color code (must be 0-255)
	 * @throws IllegalArgumentException if color code is not between 0 and 255.
	 */
	private Ansi8BitColor(String prefix, int code) {
		Assert.isTrue(code >= 0 && code <= 255, "Code must be between 0 and 255");
		this.prefix = prefix;
		this.code = code;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Ansi8BitColor other = (Ansi8BitColor) obj;
		return this.prefix.equals(other.prefix) && this.code == other.code;
	}

	@Override
	public int hashCode() {
		return this.prefix.hashCode() * 31 + this.code;
	}

	@Override
	public String toString() {
		return this.prefix + this.code;
	}

	/**
	 * Return a foreground ANSI color code instance for the given code.
	 * @param code the color code
	 * @return an ANSI color code instance
	 */
	public static Ansi8BitColor foreground(int code) {
		return new Ansi8BitColor("38;5;", code);
	}

	/**
	 * Return a background ANSI color code instance for the given code.
	 * @param code the color code
	 * @return an ANSI color code instance
	 */
	public static Ansi8BitColor background(int code) {
		return new Ansi8BitColor("48;5;", code);
	}

}
