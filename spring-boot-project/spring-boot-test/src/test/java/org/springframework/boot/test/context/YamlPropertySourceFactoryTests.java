/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.context;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link YamlPropertySourceFactory}.
 *
 * @author Dmytro Nosan
 */
class YamlPropertySourceFactoryTests {

	private final YamlPropertySourceFactory factory = new YamlPropertySourceFactory();

	@Test
	void shouldCreatePropertySourceWithYamlPropertySourceLoaderWithGivenName() throws IOException {
		EncodedResource resource = new EncodedResource(new ClassPathResource("test.yaml"));
		PropertySource<?> propertySource = this.factory.createPropertySource("test", resource);
		assertThat(propertySource.getName()).isEqualTo("test");
		assertProperties(propertySource);
	}

	@Test
	void shouldCreatePropertySourceWithYamlPropertySourceLoaderWithResourceDescriptionName() throws IOException {
		EncodedResource resource = new EncodedResource(new ClassPathResource("test.yaml"));
		PropertySource<?> propertySource = this.factory.createPropertySource(null, resource);
		assertThat(propertySource.getName()).isEqualTo("class path resource [test.yaml]");
		assertProperties(propertySource);
	}

	@Test
	void shouldCreatePropertySourceWithYamlPropertySourceLoaderWithGeneratedName() throws IOException {
		ClassPathResource resource = spy(new ClassPathResource("test.yaml"));
		willReturn(null).given(resource).getDescription();
		PropertySource<?> propertySource = this.factory.createPropertySource(null, new EncodedResource(resource));
		assertThat(propertySource.getName()).startsWith("ClassPathResource@");
		assertProperties(propertySource);
	}

	private static void assertProperties(PropertySource<?> propertySource) {
		assertThat(propertySource.getProperty("spring.bar")).isEqualTo("bar");
		assertThat(propertySource.getProperty("spring.foo")).isEqualTo("baz");
	}

}
