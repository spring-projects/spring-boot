/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocation}.
 *
 * @author Phillip Webb
 */
class ConfigDataLocationTests {

	@Test
	void isOptionalWhenNotPrefixedWithOptionalReturnsFalse() {
		ConfigDataLocation location = ConfigDataLocation.of("test");
		assertThat(location.isOptional()).isFalse();
	}

	@Test
	void isOptionalWhenPrefixedWithOptionalReturnsTrue() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:test");
		assertThat(location.isOptional()).isTrue();
	}

	@Test
	void getValueWhenNotPrefixedWithOptionalReturnsValue() {
		ConfigDataLocation location = ConfigDataLocation.of("test");
		assertThat(location.getValue()).isEqualTo("test");
	}

	@Test
	void getValueWhenPrefixedWithOptionalReturnsValueWithoutPrefix() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:test");
		assertThat(location.getValue()).isEqualTo("test");
	}

	@Test
	void hasPrefixWhenPrefixedReturnsTrue() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:test:path");
		assertThat(location.hasPrefix("test:")).isTrue();
	}

	@Test
	void hasPrefixWhenNotPrefixedReturnsFalse() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:file:path");
		assertThat(location.hasPrefix("test:")).isFalse();
	}

	@Test
	void getNonPrefixedValueWhenPrefixedReturnsNonPrefixed() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:test:path");
		assertThat(location.getNonPrefixedValue("test:")).isEqualTo("path");
	}

	@Test
	void getNonPrefixedValueWhenNotPrefixedReturnsOriginalValue() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:file:path");
		assertThat(location.getNonPrefixedValue("test:")).isEqualTo("file:path");
	}

	@Test
	void getOriginWhenNoOriginReturnsNull() {
		ConfigDataLocation location = ConfigDataLocation.of("test");
		assertThat(location.getOrigin()).isNull();
	}

	@Test
	void getOriginWhenWithOriginReturnsOrigin() {
		Origin origin = mock(Origin.class);
		ConfigDataLocation location = ConfigDataLocation.of("test").withOrigin(origin);
		assertThat(location.getOrigin()).isSameAs(origin);
	}

	@Test
	void equalsAndHashCode() {
		ConfigDataLocation l1 = ConfigDataLocation.of("a");
		ConfigDataLocation l2 = ConfigDataLocation.of("a");
		ConfigDataLocation l3 = ConfigDataLocation.of("optional:a");
		ConfigDataLocation l4 = ConfigDataLocation.of("b");
		assertThat(l1.hashCode()).isEqualTo(l2.hashCode()).isEqualTo(l3.hashCode());
		assertThat(l1).isEqualTo(l2).isEqualTo(l3).isNotEqualTo(l4);
	}

	@Test
	void toStringReturnsOriginalString() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:test");
		assertThat(location).hasToString("optional:test");
	}

	@Test
	void withOriginSetsOrigin() {
		Origin origin = mock(Origin.class);
		ConfigDataLocation location = ConfigDataLocation.of("test").withOrigin(origin);
		assertThat(location.getOrigin()).isSameAs(origin);
	}

	@Test
	void ofWhenNullValueReturnsNull() {
		assertThat(ConfigDataLocation.of(null)).isNull();
	}

	@Test
	void ofWhenEmptyValueReturnsNull() {
		assertThat(ConfigDataLocation.of("")).isNull();
	}

	@Test
	void ofWhenEmptyOptionalValueReturnsNull() {
		assertThat(ConfigDataLocation.of("optional:")).isNull();
	}

	@Test
	void ofReturnsLocation() {
		assertThat(ConfigDataLocation.of("test")).hasToString("test");
	}

	@Test
	void splitWhenNoSemiColonReturnsSingleElement() {
		ConfigDataLocation location = ConfigDataLocation.of("test");
		ConfigDataLocation[] split = location.split();
		assertThat(split).containsExactly(ConfigDataLocation.of("test"));
	}

	@Test
	void splitWhenSemiColonReturnsElements() {
		ConfigDataLocation location = ConfigDataLocation.of("one;two;three");
		ConfigDataLocation[] split = location.split();
		assertThat(split).containsExactly(ConfigDataLocation.of("one"), ConfigDataLocation.of("two"),
				ConfigDataLocation.of("three"));
	}

	@Test
	void splitOnCharReturnsElements() {
		ConfigDataLocation location = ConfigDataLocation.of("one::two::three");
		ConfigDataLocation[] split = location.split("::");
		assertThat(split).containsExactly(ConfigDataLocation.of("one"), ConfigDataLocation.of("two"),
				ConfigDataLocation.of("three"));
	}

	@Test
	void splitWhenHasOriginReturnsElementsWithOriginSet() {
		Origin origin = mock(Origin.class);
		ConfigDataLocation location = ConfigDataLocation.of("a;b").withOrigin(origin);
		ConfigDataLocation[] split = location.split();
		assertThat(split[0].getOrigin()).isEqualTo(origin);
		assertThat(split[1].getOrigin()).isEqualTo(origin);
	}

}
