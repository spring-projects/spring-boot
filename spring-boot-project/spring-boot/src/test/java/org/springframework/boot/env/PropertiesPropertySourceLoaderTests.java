/*
 * Copyright 2012-present the original author or authors.
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

import org.springframework.boot.testsupport.classpath.resources.WithResource;
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

	private final PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

	@Test
	void getFileExtensions() {
		assertThat(this.loader.getFileExtensions()).isEqualTo(new String[] { "properties", "xml" });
	}

	@Test
	@WithResource(name = "test.properties", content = "test=properties")
	void loadProperties() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties", new ClassPathResource("test.properties"));
		PropertySource<?> source = loaded.get(0);
		assertThat(source.getProperty("test")).isEqualTo("properties");
	}

	@Test
	@WithResource(name = "test.properties", content = """
			#---
			#test
			blah=hello world
			bar=baz
			hello=world
			#---
			foo=bar
			bling=biz
			#comment1
			#comment2
			""")
	void loadMultiDocumentPropertiesWithSeparatorAtTheBeginningOfFile() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties", new ClassPathResource("test.properties"));
		assertThat(loaded).hasSize(2);
		PropertySource<?> source1 = loaded.get(0);
		PropertySource<?> source2 = loaded.get(1);
		assertThat(source1.getProperty("blah")).isEqualTo("hello world");
		assertThat(source2.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	@WithResource(name = "test.properties", content = """
			#test
			blah=hello world
			bar=baz
			hello=world
			#---
			foo=bar
			bling=biz
			#comment1
			#comment2
			#---
			""")
	void loadMultiDocumentProperties() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties", new ClassPathResource("test.properties"));
		assertThat(loaded).hasSize(2);
		PropertySource<?> source1 = loaded.get(0);
		PropertySource<?> source2 = loaded.get(1);
		assertThat(source1.getProperty("blah")).isEqualTo("hello world");
		assertThat(source2.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	@WithResource(name = "test.properties", content = """

			#---
			#test
			blah=hello world
			bar=baz
			hello=world
			#---
			#---
			foo=bar
			bling=biz
			#comment1
			#comment2
			""")
	void loadMultiDocumentPropertiesWithEmptyDocument() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties", new ClassPathResource("test.properties"));
		assertThat(loaded).hasSize(2);
		PropertySource<?> source1 = loaded.get(0);
		PropertySource<?> source2 = loaded.get(1);
		assertThat(source1.getProperty("blah")).isEqualTo("hello world");
		assertThat(source2.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	@WithResource(name = "test.xml", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
			<properties>
				<entry key="test">xml</entry>
			</properties>
			""")
	void loadXml() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.xml", new ClassPathResource("test.xml"));
		PropertySource<?> source = loaded.get(0);
		assertThat(source.getProperty("test")).isEqualTo("xml");
	}

}
