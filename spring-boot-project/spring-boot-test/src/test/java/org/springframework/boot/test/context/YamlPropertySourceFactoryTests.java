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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
	void shouldCreatePropertySourceWithGivenName() throws IOException {
		EncodedResource resource = new EncodedResource(create("test.yaml"));
		PropertySource<?> propertySource = this.factory.createPropertySource("test", resource);
		assertThat(propertySource.getName()).isEqualTo("test");
		assertProperties(propertySource);
	}

	@Test
	void shouldCreatePropertySourceWithResourceDescriptionName() throws IOException {
		EncodedResource resource = new EncodedResource(create("test.yaml"));
		PropertySource<?> propertySource = this.factory.createPropertySource(null, resource);
		assertThat(propertySource.getName()).isEqualTo(resource.getResource().getDescription());
		assertProperties(propertySource);
	}

	@Test
	void shouldCreatePropertySourceWithGeneratedName() throws IOException {
		Resource resource = spy(create("test.yaml"));
		willReturn(null).given(resource).getDescription();
		PropertySource<?> propertySource = this.factory.createPropertySource(null, new EncodedResource(resource));
		assertThat(propertySource.getName()).startsWith("ClassPathResource@");
		assertProperties(propertySource);
	}

	@Test
	void shouldNotCreatePropertySourceWhenMultiDocumentYaml() {
		EncodedResource resource = new EncodedResource(create("multi.yaml"));
		assertThatIllegalArgumentException().isThrownBy(() -> this.factory.createPropertySource(null, resource))
			.withMessageContaining("is a multi-document YAML file");
	}

	@Test
	void shouldCreateEmptyPropertySourceWhenYamlFileIsEmpty() throws IOException {
		EncodedResource resource = new EncodedResource(create("empty.yaml"));
		PropertySource<?> propertySource = this.factory.createPropertySource("empty", resource);
		assertThat(propertySource.getName()).isEqualTo("empty");
		assertThat(propertySource.getSource()).asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
			.isEmpty();
	}

	private Resource create(String name) {
		return new ClassPathResource(name, getClass());
	}

	private static void assertProperties(PropertySource<?> propertySource) {
		assertThat(propertySource.getProperty("spring.bar")).isEqualTo("bar");
		assertThat(propertySource.getProperty("spring.foo")).isEqualTo("baz");
	}

}
