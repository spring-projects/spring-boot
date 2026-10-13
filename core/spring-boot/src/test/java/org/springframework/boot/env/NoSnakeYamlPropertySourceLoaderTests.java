/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.env;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link YamlPropertySourceLoader} when snakeyaml is not available.
 *
 * @author Madhura Bhave
 */
@ClassPathExclusions("snakeyaml-*.jar")
class NoSnakeYamlPropertySourceLoaderTests {

	private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	@Test
	void load() {
		ByteArrayResource resource = new ByteArrayResource("foo:\n  bar: spam".getBytes());
		assertThatIllegalStateException().isThrownBy(() -> this.loader.load("resource", resource))
			.withMessageContaining("Attempted to load resource but snakeyaml was not found on the classpath");
	}

}
