/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.loader.jar;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AsciiBytes}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class AsciiBytesTests {

	private static final char NO_SUFFIX = 0;

	@Test
	public void createFromBytes() {
		AsciiBytes bytes = new AsciiBytes(new byte[] { 65, 66 });
		assertThat(bytes.toString()).isEqualTo("AB");
	}

	@Test
	public void createFromBytesWithOffset() {
		AsciiBytes bytes = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
		assertThat(bytes.toString()).isEqualTo("BC");
	}

	@Test
	public void createFromString() {
		AsciiBytes bytes = new AsciiBytes("AB");
		assertThat(bytes.toString()).isEqualTo("AB");
	}

	@Test
	public void length() {
		AsciiBytes b1 = new AsciiBytes(new byte[] { 65, 66 });
		AsciiBytes b2 = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
		assertThat(b1.length()).isEqualTo(2);
		assertThat(b2.length()).isEqualTo(2);
	}

	@Test
	public void startWith() {
		AsciiBytes abc = new AsciiBytes(new byte[] { 65, 66, 67 });
		AsciiBytes ab = new AsciiBytes(new byte[] { 65, 66 });
		AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67 }, 1, 2);
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		assertThat(abc.startsWith(abc)).isTrue();
		assertThat(abc.startsWith(ab)).isTrue();
		assertThat(abc.startsWith(bc)).isFalse();
		assertThat(abc.startsWith(abcd)).isFalse();
	}

	@Test
	public void endsWith() {
		AsciiBytes abc = new AsciiBytes(new byte[] { 65, 66, 67 });
		AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67 }, 1, 2);
		AsciiBytes ab = new AsciiBytes(new byte[] { 65, 66 });
		AsciiBytes aabc = new AsciiBytes(new byte[] { 65, 65, 66, 67 });
		assertThat(abc.endsWith(abc)).isTrue();
		assertThat(abc.endsWith(bc)).isTrue();
		assertThat(abc.endsWith(ab)).isFalse();
		assertThat(abc.endsWith(aabc)).isFalse();
	}

	@Test
	public void substringFromBeingIndex() {
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		assertThat(abcd.substring(0).toString()).isEqualTo("ABCD");
		assertThat(abcd.substring(1).toString()).isEqualTo("BCD");
		assertThat(abcd.substring(2).toString()).isEqualTo("CD");
		assertThat(abcd.substring(3).toString()).isEqualTo("D");
		assertThat(abcd.substring(4).toString()).isEqualTo("");
		assertThatExceptionOfType(IndexOutOfBoundsException.class)
				.isThrownBy(() -> abcd.substring(5));
	}

	@Test
	public void substring() {
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		assertThat(abcd.substring(0, 4).toString()).isEqualTo("ABCD");
		assertThat(abcd.substring(1, 3).toString()).isEqualTo("BC");
		assertThat(abcd.substring(3, 4).toString()).isEqualTo("D");
		assertThat(abcd.substring(3, 3).toString()).isEqualTo("");
		assertThatExceptionOfType(IndexOutOfBoundsException.class)
				.isThrownBy(() -> abcd.substring(3, 5));
	}

	@Test
	public void hashCodeAndEquals() {
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		AsciiBytes bc = new AsciiBytes(new byte[] { 66, 67 });
		AsciiBytes bc_substring = new AsciiBytes(new byte[] { 65, 66, 67, 68 })
				.substring(1, 3);
		AsciiBytes bc_string = new AsciiBytes("BC");
		assertThat(bc.hashCode()).isEqualTo(bc.hashCode());
		assertThat(bc.hashCode()).isEqualTo(bc_substring.hashCode());
		assertThat(bc.hashCode()).isEqualTo(bc_string.hashCode());
		assertThat(bc).isEqualTo(bc);
		assertThat(bc).isEqualTo(bc_substring);
		assertThat(bc).isEqualTo(bc_string);
		assertThat(bc.hashCode()).isNotEqualTo(abcd.hashCode());
		assertThat(bc).isNotEqualTo(abcd);
	}

	@Test
	public void hashCodeSameAsString() {
		hashCodeSameAsString("abcABC123xyz!");
	}

	@Test
	public void hashCodeSameAsStringWithSpecial() {
		hashCodeSameAsString("special/\u00EB.dat");
	}

	@Test
	public void hashCodeSameAsStringWithCyrillicCharacters() {
		hashCodeSameAsString("\u0432\u0435\u0441\u043D\u0430");
	}

	@Test
	public void hashCodeSameAsStringWithEmoji() {
		hashCodeSameAsString("\ud83d\udca9");
	}

	private void hashCodeSameAsString(String input) {
		assertThat(new AsciiBytes(input).hashCode()).isEqualTo(input.hashCode());
	}

	@Test
	public void matchesSameAsString() {
		matchesSameAsString("abcABC123xyz!");
	}

	@Test
	public void matchesSameAsStringWithSpecial() {
		matchesSameAsString("special/\u00EB.dat");
	}

	@Test
	public void matchesSameAsStringWithCyrillicCharacters() {
		matchesSameAsString("\u0432\u0435\u0441\u043D\u0430");
	}

	@Test
	public void matchesDifferentLengths() {
		assertThat(new AsciiBytes("abc").matches("ab", NO_SUFFIX)).isFalse();
		assertThat(new AsciiBytes("abc").matches("abcd", NO_SUFFIX)).isFalse();
		assertThat(new AsciiBytes("abc").matches("abc", NO_SUFFIX)).isTrue();
		assertThat(new AsciiBytes("abc").matches("a", 'b')).isFalse();
		assertThat(new AsciiBytes("abc").matches("abc", 'd')).isFalse();
		assertThat(new AsciiBytes("abc").matches("ab", 'c')).isTrue();
	}

	@Test
	public void matchesSuffix() {
		assertThat(new AsciiBytes("ab").matches("a", 'b')).isTrue();
	}

	@Test
	public void matchesSameAsStringWithEmoji() {
		matchesSameAsString("\ud83d\udca9");
	}

	@Test
	public void hashCodeFromInstanceMatchesHashCodeFromString() {
		String name = "fonts/宋体/simsun.ttf";
		assertThat(new AsciiBytes(name).hashCode()).isEqualTo(AsciiBytes.hashCode(name));
	}

	@Test
	public void instanceCreatedFromCharSequenceMatchesSameCharSequence() {
		String name = "fonts/宋体/simsun.ttf";
		assertThat(new AsciiBytes(name).matches(name, NO_SUFFIX)).isTrue();
	}

	private void matchesSameAsString(String input) {
		assertThat(new AsciiBytes(input).matches(input, NO_SUFFIX)).isTrue();
	}

}
