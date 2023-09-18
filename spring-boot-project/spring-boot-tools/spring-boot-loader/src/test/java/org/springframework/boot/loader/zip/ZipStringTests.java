/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipString}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ZipStringTests {

	@ParameterizedTest
	@EnumSource
	void hashGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
		testHash(sourceType, true, "abcABC123xyz!");
		testHash(sourceType, false, "abcABC123xyz!");
	}

	@ParameterizedTest
	@EnumSource
	void hashWhenHasSpecialCharsGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
		testHash(sourceType, true, "special/\u00EB.dat");
	}

	@ParameterizedTest
	@EnumSource
	void hashWhenHasCyrillicCharsGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
		testHash(sourceType, true, "\u0432\u0435\u0441\u043D\u0430");
	}

	@ParameterizedTest
	@EnumSource
	void hashWhenHasEmojiGeneratesCorrectHashCode(HashSourceType sourceType) throws Exception {
		testHash(sourceType, true, "\ud83d\udca9");
	}

	@ParameterizedTest
	@EnumSource
	void hashWhenOnlyDifferenceIsEndSlashGeneratesSameHashCode(HashSourceType sourceType) throws Exception {
		testHash(sourceType, "", true, "/".hashCode());
		testHash(sourceType, "/", true, "/".hashCode());
		testHash(sourceType, "a/b", true, "a/b/".hashCode());
		testHash(sourceType, "a/b/", true, "a/b/".hashCode());
	}

	void testHash(HashSourceType sourceType, boolean addSlash, String source) throws Exception {
		String expected = (addSlash && !source.endsWith("/")) ? source + "/" : source;
		testHash(sourceType, source, addSlash, expected.hashCode());
	}

	void testHash(HashSourceType sourceType, String source, boolean addEndSlash, int expected) throws Exception {
		switch (sourceType) {
			case STRING -> {
				assertThat(ZipString.hash(source, addEndSlash)).isEqualTo(expected);
			}
			case CHAR_SEQUENCE -> {
				CharSequence charSequence = new StringBuilder(source);
				assertThat(ZipString.hash(charSequence, addEndSlash)).isEqualTo(expected);
			}
			case DATA_BLOCK -> {
				ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
				assertThat(ZipString.hash(null, dataBlock, 0, (int) dataBlock.size(), addEndSlash)).isEqualTo(expected);

			}
		}
	}

	@Test
	void matchesWhenExactMatchReturnsTrue() throws Exception {
		assertMatches("one/two/three", "one/two/three", false).isTrue();
	}

	@Test
	void matchesWhenNotMatchWithSameLengthReturnsFalse() throws Exception {
		assertMatches("one/two/three", "one/too/three", false).isFalse();
	}

	@Test
	void matchesWhenExactMatchWithSpecialCharsReturnsTrue() throws Exception {
		assertMatches("special/\u00EB.dat", "special/\u00EB.dat", false).isTrue();
	}

	@Test
	void matchesWhenExactMatchWithCyrillicCharsReturnsTrue() throws Exception {
		assertMatches("\u0432\u0435\u0441\u043D\u0430", "\u0432\u0435\u0441\u043D\u0430", false).isTrue();
	}

	@Test
	void matchesWhenNoMatchWithCyrillicCharsReturnsFalse() throws Exception {
		assertMatches("\u0432\u0435\u0441\u043D\u0430", "\u0432\u0435\u0441\u043D\u043D", false).isFalse();
	}

	@Test
	void matchesWhenExactMatchWithEmojiCharsReturnsTrue() throws Exception {
		assertMatches("\ud83d\udca9", "\ud83d\udca9", false).isTrue();
	}

	@Test
	void matchesWithAddSlash() throws Exception {
		assertMatches("META-INF/MANFIFEST.MF", "META-INF/MANFIFEST.MF", true).isTrue();
		assertMatches("one/two/three/", "one/two/three", true).isTrue();
		assertMatches("one/two/three", "one/two/three/", true).isFalse();
		assertMatches("one/two/three/", "one/too/three", true).isFalse();
		assertMatches("one/two/three", "one/too/three/", true).isFalse();
		assertMatches("one/two/three//", "one/two/three", true).isFalse();
		assertMatches("one/two/three", "one/two/three//", true).isFalse();
	}

	@Test
	void matchesWhenDataBlockShorterThenCharSequenceReturnsFalse() throws Exception {
		assertMatches("one/two/thre", "one/two/three", false).isFalse();
	}

	@Test
	void matchesWhenCharSequenceShorterThanDataBlockReturnsFalse() throws Exception {
		assertMatches("one/two/three", "one/two/thre", false).isFalse();
	}

	@Test
	void startsWithWhenStartsWith() throws Exception {
		assertStartsWith("one/two", "one/").isEqualTo(4);
	}

	@Test
	void startsWithWhenExact() throws Exception {
		assertStartsWith("one/", "one/").isEqualTo(4);
	}

	@Test
	void startsWithWhenTooShort() throws Exception {
		assertStartsWith("one/two", "one/two/three/").isEqualTo(-1);
	}

	@Test
	void startsWithWhenDoesNotStartWith() throws Exception {
		assertStartsWith("one/three/", "one/two/").isEqualTo(-1);
	}

	@Test
	void zipStringWhenMultiCodePointAtBufferBoundary() throws Exception {
		StringBuilder source = new StringBuilder();
		for (int i = 0; i < ZipString.BUFFER_SIZE - 1; i++) {
			source.append("A");
		}
		source.append("\u1EFF");
		String charSequence = source.toString();
		source.append("suffix");
		assertStartsWith(source.toString(), charSequence);
	}

	private AbstractBooleanAssert<?> assertMatches(String source, CharSequence charSequence, boolean addSlash)
			throws Exception {
		ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
		return assertThat(ZipString.matches(null, dataBlock, 0, (int) dataBlock.size(), charSequence, addSlash));
	}

	private AbstractIntegerAssert<?> assertStartsWith(String source, CharSequence charSequence) throws IOException {
		ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
		return assertThat(ZipString.startsWith(null, dataBlock, 0, (int) dataBlock.size(), charSequence));
	}

	enum HashSourceType {

		STRING, CHAR_SEQUENCE, DATA_BLOCK

	}

}
