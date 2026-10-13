/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResource} when used on a super-class.
 *
 * @author Andy Wilkinson
 */
class OnSuperClassWithResourceTests extends WithResourceClass {

	@Test
	void whenWithResourceIsUsedOnASuperClassThenResourceIsAvailable() throws IOException {
		assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("super-class content");
	}

	@Test
	@WithResource(name = "method-resource", content = "method")
	void whenWithResourceIsUsedOnASuperClassAndMethodThenBothResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("super-class content");
		assertThat(new ClassPathResource("method-resource").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method");
	}

	@Test
	@WithResource(name = "method-resource-1", content = "method-1")
	@WithResource(name = "method-resource-2", content = "method-2")
	void whenWithResourceIsUsedOnASuperClassAndRepeatedOnMethodThenAllResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("super-class content");
		assertThat(new ClassPathResource("method-resource-1").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method-1");
		assertThat(new ClassPathResource("method-resource-2").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method-2");
	}

}
