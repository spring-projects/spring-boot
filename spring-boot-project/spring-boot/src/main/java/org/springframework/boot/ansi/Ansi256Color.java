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

/**
 * {@link AnsiElement} implementation for Ansi 256 colors.
 * <p>
 * use {@link Ansi256Color.Foreground} or {@link Ansi256Color.Background} as a concrete
 * class.
 *
 * @author Toshiaki Maki
 * @since 2.2.0
 */
public abstract class Ansi256Color implements AnsiElement {

	/**
	 * color code
	 */
	final int colorCode;

	/**
	 * @param colorCode color code (must be 0-255)
	 * @throws IllegalArgumentException if color code is not between 0 and 255.
	 */
	Ansi256Color(int colorCode) {
		if (colorCode < 0 || colorCode > 255) {
			throw new IllegalArgumentException("'colorCode' must be between 0 and 255.");
		}
		this.colorCode = colorCode;
	}

	/**
	 * {@link Ansi256Color} foreground colors.
	 *
	 * @author Toshiaki Maki
	 * @since 2.2.0
	 */
	public static class Foreground extends Ansi256Color {

		public Foreground(int colorCode) {
			super(colorCode);
		}

		@Override
		public String toString() {
			return "38;5;" + super.colorCode;
		}

	}

	/**
	 * {@link Ansi256Color} background colors.
	 *
	 * @author Toshiaki Maki
	 * @since 2.2.0
	 */
	public static class Background extends Ansi256Color {

		public Background(int colorCode) {
			super(colorCode);
		}

		@Override
		public String toString() {
			return "48;5;" + super.colorCode;
		}

	}

}
