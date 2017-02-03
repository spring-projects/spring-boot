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

package org.springframework.boot.env;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CanonicalPropertyName}.
 *
 * @author Phillip Webb
 */
public class CanonicalPropertyNameTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createShouldCreateWithName() throws Exception {
		CanonicalPropertyName name = new CanonicalPropertyName("foo.baz.bar");
		assertThat(name.toString()).isEqualTo("foo.baz.bar");
	}

	@Test
	public void createWhenIndexIntegerShouldCreateWithName() throws Exception {
		CanonicalPropertyName name = new CanonicalPropertyName("foo[1].baz[2].bar");
		assertThat(name.toString()).isEqualTo("foo[1].baz[2].bar");
	}

	@Test
	public void createWhenIndexStringShouldCreateWithName() throws Exception {
		CanonicalPropertyName name = new CanonicalPropertyName("foo[baz].bar");
		assertThat(name.toString()).isEqualTo("foo[baz].bar");
	}

	@Test
	public void createWhenSingleLetterNameShouldCreateWithName() throws Exception {
		CanonicalPropertyName name = new CanonicalPropertyName("a");
		assertThat(name.toString()).isEqualTo("a");
	}

	@Test
	public void createWhenNameIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be empty");
		new CanonicalPropertyName(null);
	}

	@Test
	public void createWhenNameIsEmptyShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be empty");
		new CanonicalPropertyName(null);
	}

	@Test
	public void createWhenNameIsNotLowercaseShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \"naMe\" is not canonical");
		new CanonicalPropertyName("naMe");
	}

	@Test
	public void createWhenNameStartsWithNumberShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \"foo.1bar.baz\" is not canonical");
		new CanonicalPropertyName("foo.1bar.baz");
	}

	@Test
	public void createWhenNameStartsWithWhitespaceShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \" foo\" is not canonical");
		new CanonicalPropertyName(" foo");
	}

	@Test
	public void createWhenNameStartsWithDotShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \".foo\" is not canonical");
		new CanonicalPropertyName(".foo");
	}

	@Test
	public void createWhenNameEndsWithDotShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \"foo.\" is not canonical");
		new CanonicalPropertyName("foo.");
	}

	@Test
	public void createWhenNameContainsDoubleDotShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \"foo..bar\" is not canonical");
		new CanonicalPropertyName("foo..bar");
	}

	@Test
	public void createWhenNameContainsUnderscoreShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \"foo_bar\" is not canonical");
		new CanonicalPropertyName("foo_bar");
	}

	@Test
	public void createWhenNameContainsDashShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name \"foo-bar\" is not canonical");
		new CanonicalPropertyName("foo-bar");
	}

	@Test
	public void hashCodeWhenSameNameShouldReturnSame() throws Exception {
		CanonicalPropertyName n1 = new CanonicalPropertyName("abc");
		CanonicalPropertyName n2 = new CanonicalPropertyName("abc");
		assertThat(n1.hashCode()).isEqualTo(n2.hashCode());
	}

	@Test
	public void equalsWhenSameNameShouldReturnTrue() throws Exception {
		CanonicalPropertyName n1 = new CanonicalPropertyName("abc");
		CanonicalPropertyName n2 = new CanonicalPropertyName("abc");
		assertThat(n1).isEqualTo(n2);
	}

	@Test
	public void equalsWhenDifferentNameShouldReturnFalse() throws Exception {
		CanonicalPropertyName n1 = new CanonicalPropertyName("abc");
		CanonicalPropertyName n2 = new CanonicalPropertyName("bcd");
		assertThat(n1).isNotEqualTo(n2);
	}

}
