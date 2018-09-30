/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringSequence}.
 *
 * @author Phillip Webb
 */
public class StringSequenceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenSourceIsNullShouldThrowException() {
		this.thrown.expect(NullPointerException.class);
		this.thrown.expectMessage("Source must not be null");
		new StringSequence(null);
	}

	@Test
	public void createWithIndexWhenSourceIsNullShouldThrowException() {
		this.thrown.expect(NullPointerException.class);
		this.thrown.expectMessage("Source must not be null");
		new StringSequence(null, 0, 0);
	}

	@Test
	public void createWhenStartIsLessThanZeroShouldThrowException() {
		this.thrown.expect(StringIndexOutOfBoundsException.class);
		new StringSequence("x", -1, 0);
	}

	@Test
	public void createWhenEndIsGreaterThanLengthShouldThrowException() {
		this.thrown.expect(StringIndexOutOfBoundsException.class);
		new StringSequence("x", 0, 2);
	}

	@Test
	public void createFromString() {
		assertThat(new StringSequence("test").toString()).isEqualTo("test");
	}

	@Test
	public void subSequenceWithJustStartShouldReturnSubSequence() {
		assertThat(new StringSequence("smiles").subSequence(1).toString())
				.isEqualTo("miles");
	}

	@Test
	public void subSequenceShouldReturnSubSequence() {
		assertThat(new StringSequence("hamburger").subSequence(4, 8).toString())
				.isEqualTo("urge");
		assertThat(new StringSequence("smiles").subSequence(1, 5).toString())
				.isEqualTo("mile");
	}

	@Test
	public void subSequenceWhenCalledMultipleTimesShouldReturnSubSequence() {
		assertThat(new StringSequence("hamburger").subSequence(4, 8).subSequence(1, 3)
				.toString()).isEqualTo("rg");
	}

	@Test
	public void subSequenceWhenEndPastExistingEndShouldThrowException() {
		StringSequence sequence = new StringSequence("abcde").subSequence(1, 4);
		assertThat(sequence.toString()).isEqualTo("bcd");
		assertThat(sequence.subSequence(2, 3).toString()).isEqualTo("d");
		this.thrown.expect(IndexOutOfBoundsException.class);
		sequence.subSequence(3, 4);
	}

	@Test
	public void subSequenceWhenStartPastExistingEndShouldThrowException() {
		StringSequence sequence = new StringSequence("abcde").subSequence(1, 4);
		assertThat(sequence.toString()).isEqualTo("bcd");
		assertThat(sequence.subSequence(2, 3).toString()).isEqualTo("d");
		this.thrown.expect(IndexOutOfBoundsException.class);
		sequence.subSequence(4, 3);
	}

	@Test
	public void isEmptyWhenEmptyShouldReturnTrue() {
		assertThat(new StringSequence("").isEmpty()).isTrue();
	}

	@Test
	public void isEmptyWhenNotEmptyShouldReturnFalse() {
		assertThat(new StringSequence("x").isEmpty()).isFalse();
	}

	@Test
	public void lengthShouldReturnLength() {
		StringSequence sequence = new StringSequence("hamburger");
		assertThat(sequence.length()).isEqualTo(9);
		assertThat(sequence.subSequence(4, 8).length()).isEqualTo(4);
	}

	@Test
	public void charAtShouldReturnChar() {
		StringSequence sequence = new StringSequence("hamburger");
		assertThat(sequence.charAt(0)).isEqualTo('h');
		assertThat(sequence.charAt(1)).isEqualTo('a');
		assertThat(sequence.subSequence(4, 8).charAt(0)).isEqualTo('u');
		assertThat(sequence.subSequence(4, 8).charAt(1)).isEqualTo('r');
	}

	@Test
	public void indexOfCharShouldReturnIndexOf() {
		StringSequence sequence = new StringSequence("aabbaacc");
		assertThat(sequence.indexOf('a')).isEqualTo(0);
		assertThat(sequence.indexOf('b')).isEqualTo(2);
		assertThat(sequence.subSequence(2).indexOf('a')).isEqualTo(2);
	}

	@Test
	public void indexOfStringShouldReturnIndexOf() {
		StringSequence sequence = new StringSequence("aabbaacc");
		assertThat(sequence.indexOf("a")).isEqualTo(0);
		assertThat(sequence.indexOf("b")).isEqualTo(2);
		assertThat(sequence.subSequence(2).indexOf("a")).isEqualTo(2);
	}

	@Test
	public void indexOfStringFromIndexShouldReturnIndexOf() {
		StringSequence sequence = new StringSequence("aabbaacc");
		assertThat(sequence.indexOf("a", 2)).isEqualTo(4);
		assertThat(sequence.indexOf("b", 3)).isEqualTo(3);
		assertThat(sequence.subSequence(2).indexOf("a", 3)).isEqualTo(3);
	}

	@Test
	public void hashCodeShouldBeSameAsString() {
		assertThat(new StringSequence("hamburger").hashCode())
				.isEqualTo("hamburger".hashCode());
		assertThat(new StringSequence("hamburger").subSequence(4, 8).hashCode())
				.isEqualTo("urge".hashCode());
	}

	@Test
	public void equalsWhenSameContentShouldMatch() {
		StringSequence a = new StringSequence("hamburger").subSequence(4, 8);
		StringSequence b = new StringSequence("urge");
		StringSequence c = new StringSequence("urgh");
		assertThat(a).isEqualTo(b).isNotEqualTo(c);
	}

	@Test
	public void startsWithWhenExactMatch() {
		assertThat(new StringSequence("abc").startsWith("abc")).isTrue();
	}

	@Test
	public void startsWithWhenLongerAndStartsWith() {
		assertThat(new StringSequence("abcd").startsWith("abc")).isTrue();
	}

	@Test
	public void startsWithWhenLongerAndDoesNotStartWith() {
		assertThat(new StringSequence("abcd").startsWith("abx")).isFalse();
	}

	@Test
	public void startsWithWhenShorterAndDoesNotStartWith() {
		assertThat(new StringSequence("ab").startsWith("abc")).isFalse();
		assertThat(new StringSequence("ab").startsWith("c")).isFalse();
	}

	@Test
	public void startsWithOffsetWhenExactMatch() {
		assertThat(new StringSequence("xabc").startsWith("abc", 1)).isTrue();
	}

	@Test
	public void startsWithOffsetWhenLongerAndStartsWith() {
		assertThat(new StringSequence("xabcd").startsWith("abc", 1)).isTrue();
	}

	@Test
	public void startsWithOffsetWhenLongerAndDoesNotStartWith() {
		assertThat(new StringSequence("xabcd").startsWith("abx", 1)).isFalse();
	}

	@Test
	public void startsWithOffsetWhenShorterAndDoesNotStartWith() {
		assertThat(new StringSequence("xab").startsWith("abc", 1)).isFalse();
		assertThat(new StringSequence("xab").startsWith("c", 1)).isFalse();
	}

}
