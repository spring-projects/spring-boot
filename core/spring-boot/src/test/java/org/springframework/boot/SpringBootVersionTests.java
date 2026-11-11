/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootVersion}.
 *
 * @author Andy Wilkinson
 */
class SpringBootVersionTests {

	@Test
	void getVersionShouldReturnVersionMatchingGradleProperties() throws IOException {
		String expectedVersion = PropertiesLoaderUtils.loadProperties(new FileSystemResource(findGradleProperties()))
			.getProperty("version");
		assertThat(SpringBootVersion.getVersion()).isEqualTo(expectedVersion);
	}

	private File findGradleProperties() {
		File current = new File(".").getAbsoluteFile();
		while (current != null) {
			File gradleProperties = new File(current, "gradle.properties");
			System.out.println(gradleProperties);
			if (gradleProperties.isFile()) {
				return gradleProperties;
			}
			current = current.getParentFile();
		}
		throw new IllegalStateException("Could not find gradle.properties");
	}

}
