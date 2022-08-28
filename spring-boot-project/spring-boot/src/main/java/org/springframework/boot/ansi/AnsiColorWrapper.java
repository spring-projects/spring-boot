/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.util.ObjectUtils;

import java.util.Objects;

/**
 * ANSI Color Wrapper
 *
 * @author TomXin
 * @since 3.0.0
 */
public class AnsiColorWrapper {

	private final int code;

	private final AnsiColors.BitDepth bitDepth;

	/**
	 * Create a new {@link AnsiColorWrapper} instance with the specified bit depth.
	 * @param bitDepth the required bit depth
	 * @param code Color code, when the bit depth is 4bit, the value range of code is
	 * [30~37], [90~97]. When the bit depth is 8bit, the code value range is [0~255]
	 */
	public AnsiColorWrapper(int code, AnsiColors.BitDepth bitDepth) {
		if (bitDepth == AnsiColors.BitDepth.FOUR) {
			Assert.isTrue((30 <= code && code <= 37) || (90 <= code && code <= 97),
					"The value of 4 bit color only supported [30~37],[90~97].");
		}
		Assert.isTrue((0 <= code && code <= 255), "The value of 8 bit color only supported [0~255].");
		this.code = code;
		this.bitDepth = bitDepth;
	}

	/**
	 * Convert to {@link AnsiElement} instance
	 * @param foreOrBack foreground or background
	 * @return {@link AnsiElement} instance
	 */
	public AnsiElement toAnsiElement(ForeOrBack foreOrBack) {
		if (bitDepth == AnsiColors.BitDepth.FOUR) {
			if (foreOrBack == ForeOrBack.FORE) {
				for (AnsiColor item : AnsiColor.values()) {
					if (ObjectUtils.nullSafeEquals(item.toString(), String.valueOf(this.code))) {
						return item;
					}
				}
				throw new IllegalArgumentException(String.format("No matched AnsiColor instance,code= %d", this.code));
			}
			for (AnsiBackground item : AnsiBackground.values()) {
				if (ObjectUtils.nullSafeEquals(item.toString(), String.valueOf(this.code + 10))) {
					return item;
				}
			}
			throw new IllegalArgumentException(String.format("No matched Background instance,code= %d", this.code));
		}
		if (foreOrBack == ForeOrBack.FORE) {
			return Ansi8BitColor.foreground(this.code);
		}
		return Ansi8BitColor.background(this.code);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AnsiColorWrapper that = (AnsiColorWrapper) o;
		return this.code == that.code && this.bitDepth == that.bitDepth;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.code, this.bitDepth);
	}

}
