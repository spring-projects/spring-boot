/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesPropertySourceLoader}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class PropertiesPropertySourceLoaderTests {

	private PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

	@Test
	void getFileExtensions() {
		assertThat(this.loader.getFileExtensions()).isEqualTo(new String[] { "properties", "xml" });
	}

	@Test
	void loadProperties() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties",
				new ClassPathResource("test-properties.properties", getClass()));
		PropertySource<?> source = loaded.get(0);
		assertThat(source.getProperty("test")).isEqualTo("properties");
	}

	@Test
	void loadMultiDocumentPropertiesWithSeparatorAtTheBeginningOfFile() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties",
				new ClassPathResource("multi-document-properties-2.properties", getClass()));
		assertThat(loaded.size()).isEqualTo(2);
		PropertySource<?> source1 = loaded.get(0);
		PropertySource<?> source2 = loaded.get(1);
		assertThat(source1.getProperty("blah")).isEqualTo("hello world");
		assertThat(source2.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void loadMultiDocumentProperties() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties",
				new ClassPathResource("multi-document-properties.properties", getClass()));
		assertThat(loaded.size()).isEqualTo(2);
		PropertySource<?> source1 = loaded.get(0);
		PropertySource<?> source2 = loaded.get(1);
		assertThat(source1.getProperty("blah")).isEqualTo("hello world");
		assertThat(source2.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void loadMultiDocumentPropertiesWithEmptyDocument() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties",
				new ClassPathResource("multi-document-properties-empty.properties", getClass()));
		assertThat(loaded.size()).isEqualTo(2);
		PropertySource<?> source1 = loaded.get(0);
		PropertySource<?> source2 = loaded.get(1);
		assertThat(source1.getProperty("blah")).isEqualTo("hello world");
		assertThat(source2.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void loadXml() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.xml",
				new ClassPathResource("test-xml.xml", getClass()));
		PropertySource<?> source = loaded.get(0);
		assertThat(source.getProperty("test")).isEqualTo("xml");
	}

}
