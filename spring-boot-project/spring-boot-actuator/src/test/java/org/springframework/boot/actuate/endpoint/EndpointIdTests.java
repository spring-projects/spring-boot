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

package org.springframework.boot.actuate.endpoint;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointId}.
 *
 * @author Phillip Webb
 */
public class EndpointIdTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofWhenNullThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be empty");
		EndpointId.of(null);
	}

	@Test
	public void ofWhenEmptyThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be empty");
		EndpointId.of("");
	}

	@Test
	public void ofWhenContainsSlashThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must only contain valid chars");
		EndpointId.of("foo/bar");
	}

	@Test
	public void ofWhenHasBadCharThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must only contain valid chars");
		EndpointId.of("foo!bar");
	}

	@Test
	public void ofWhenStartsWithNumberThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not start with a number");
		EndpointId.of("1foo");
	}

	@Test
	public void ofWhenStartsWithUppercaseLetterThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not start with an uppercase letter");
		EndpointId.of("Foo");
	}

	@Test
	public void ofWhenContainsDotIsValid() {
		// Ideally we wouldn't support this but there are existing endpoints using the
		// pattern. See gh-14773
		EndpointId endpointId = EndpointId.of("foo.bar");
		assertThat(endpointId.toString()).isEqualTo("foo.bar");
	}

	@Test
	public void ofWhenContainsDashIsValid() {
		// Ideally we wouldn't support this but there are existing endpoints using the
		// pattern. See gh-14773
		EndpointId endpointId = EndpointId.of("foo-bar");
		assertThat(endpointId.toString()).isEqualTo("foo-bar");
	}

	@Test
	public void equalsAndHashCode() {
		EndpointId one = EndpointId.of("foobar1");
		EndpointId two = EndpointId.of("fooBar1");
		EndpointId three = EndpointId.of("foo-bar1");
		EndpointId four = EndpointId.of("foo.bar1");
		EndpointId five = EndpointId.of("barfoo1");
		EndpointId six = EndpointId.of("foobar2");
		assertThat(one.hashCode()).isEqualTo(two.hashCode());
		assertThat(one).isEqualTo(one).isEqualTo(two).isEqualTo(three).isEqualTo(four).isNotEqualTo(five)
				.isNotEqualTo(six);
	}

	@Test
	public void toLowerCaseStringReturnsLowercase() {
		assertThat(EndpointId.of("fooBar").toLowerCaseString()).isEqualTo("foobar");
	}

	@Test
	public void toStringReturnsString() {
		assertThat(EndpointId.of("fooBar").toString()).isEqualTo("fooBar");
	}

	@Test
	public void fromPropertyValueStripsDashes() {
		EndpointId fromPropertyValue = EndpointId.fromPropertyValue("foo-bar");
		assertThat(fromPropertyValue).isEqualTo(EndpointId.of("fooBar"));
	}

}
