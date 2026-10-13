/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResource} when used on a class.
 *
 * @author Andy Wilkinson
 */
@WithResource(name = "on-class", content = "class content")
class OnClassWithResourceTests {

	@Test
	void whenWithResourceIsUsedOnAClassThenResourceIsAvailable() throws IOException {
		assertThat(new ClassPathResource("on-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("class content");
	}

	@Test
	@WithResource(name = "method-resource", content = "method")
	void whenWithResourceIsUsedOnClassAndMethodThenBothResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("on-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("class content");
		assertThat(new ClassPathResource("method-resource").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method");
	}

	@Test
	@WithResource(name = "method-resource-1", content = "method-1")
	@WithResource(name = "method-resource-2", content = "method-2")
	void whenWithResourceIsUsedOnClassAndRepeatedOnMethodThenAllResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("on-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("class content");
		assertThat(new ClassPathResource("method-resource-1").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method-1");
		assertThat(new ClassPathResource("method-resource-2").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method-2");
	}

	@Nested
	class NestedTests {

		@Test
		void whenWithResourceIsUsedOnEnclosingClassThenResourceIsAvailable() throws IOException {
			assertThat(new ClassPathResource("on-class").getContentAsString(StandardCharsets.UTF_8))
				.isEqualTo("class content");
		}

	}

	@Nested
	@WithResource(name = "on-nested-class", content = "nested class content")
	class WithResourceNestedTests {

		@Test
		void whenWithResourceIsUsedOnEnclosingClassAndClassThenBothResourcesAreAvailable() throws IOException {
			assertThat(new ClassPathResource("on-class").getContentAsString(StandardCharsets.UTF_8))
				.isEqualTo("class content");
			assertThat(new ClassPathResource("on-nested-class").getContentAsString(StandardCharsets.UTF_8))
				.isEqualTo("nested class content");
		}

	}

}
