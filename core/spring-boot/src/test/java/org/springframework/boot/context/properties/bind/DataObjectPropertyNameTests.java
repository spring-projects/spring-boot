/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataObjectPropertyName}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DataObjectPropertyNameTests {

	@Test
	void toDashedCaseConvertsValue() {
		assertThat(DataObjectPropertyName.toDashedForm("Foo")).isEqualTo("foo");
		assertThat(DataObjectPropertyName.toDashedForm("foo")).isEqualTo("foo");
		assertThat(DataObjectPropertyName.toDashedForm("fooBar")).isEqualTo("foo-bar");
		assertThat(DataObjectPropertyName.toDashedForm("foo_bar")).isEqualTo("foo-bar");
		assertThat(DataObjectPropertyName.toDashedForm("_foo_bar")).isEqualTo("-foo-bar");
		assertThat(DataObjectPropertyName.toDashedForm("foo_Bar")).isEqualTo("foo-bar");
	}

	@Test
	void toDashedFormWhenContainsIndexedAddsNoDashToIndex() {
		assertThat(DataObjectPropertyName.toDashedForm("test[fooBar]")).isEqualTo("test[fooBar]");
		assertThat(DataObjectPropertyName.toDashedForm("testAgain[fooBar]")).isEqualTo("test-again[fooBar]");
	}

}
