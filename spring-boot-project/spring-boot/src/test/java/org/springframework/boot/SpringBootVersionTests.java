/*
 * Copyright 2012-2022 the original author or authors.
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
