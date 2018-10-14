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
	public void ofWhenNullThorowsException() {
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
	public void ofWhenContainsDashThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must be alpha-numeric");
		EndpointId.of("foo-bar");
	}

	@Test
	public void ofWhenHasBadCharThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must be alpha-numeric");
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
	public void equalsAndHashCode() {
		EndpointId one = EndpointId.of("foobar");
		EndpointId two = EndpointId.of("fooBar");
		EndpointId three = EndpointId.of("barfoo");
		assertThat(one.hashCode()).isEqualTo(two.hashCode());
		assertThat(one).isEqualTo(one).isEqualTo(two).isNotEqualTo(three);
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
