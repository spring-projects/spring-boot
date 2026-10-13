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
 * Tests for {@link WithPackageResources}.
 *
 * @author Andy Wilkinson
 */
class WithPackageResourcesTests {

	@Test
	@WithPackageResources({ "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" })
	void whenWithPackageResourcesIsUsedOnAMethodThenResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("resource-1.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("one");
		assertThat(new ClassPathResource("resource-2.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("two");
		assertThat(new ClassPathResource("sub/resource-3.txt").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("three");
	}

	@Test
	@WithPackageResources("sub/resource-3.txt")
	void whenWithPackageResourcesOnlyIncludesSomeResourcesThenOnlyIncludedResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("resource-1.txt").exists()).isFalse();
		assertThat(new ClassPathResource("resource-2.txt").exists()).isFalse();
		assertThat(new ClassPathResource("sub/resource-3.txt").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("three");
	}

}
