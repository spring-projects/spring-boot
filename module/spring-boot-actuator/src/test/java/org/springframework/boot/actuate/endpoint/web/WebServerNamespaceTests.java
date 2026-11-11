/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServerNamespace}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class WebServerNamespaceTests {

	@Test
	void fromWhenValueHasText() {
		assertThat(WebServerNamespace.from("management")).isEqualTo(WebServerNamespace.MANAGEMENT);
	}

	@Test
	void fromWhenValueIsNull() {
		assertThat(WebServerNamespace.from((String) null)).isEqualTo(WebServerNamespace.SERVER);
	}

	@Test
	void fromWhenValueIsEmpty() {
		assertThat(WebServerNamespace.from("")).isEqualTo(WebServerNamespace.SERVER);
	}

	@Test
	void namespaceWithSameValueAreEqual() {
		assertThat(WebServerNamespace.from("value")).isEqualTo(WebServerNamespace.from("value"));
	}

	@Test
	void namespaceWithDifferentValuesAreNotEqual() {
		assertThat(WebServerNamespace.from("value")).isNotEqualTo(WebServerNamespace.from("other"));
	}

	@Test
	void toStringReturnsString() {
		assertThat(WebServerNamespace.from("value")).hasToString("value");
	}

}
