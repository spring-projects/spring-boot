/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesPropertySourceLoader}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class PropertiesPropertySourceLoaderTests {

	private PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

	@Test
	public void getFileExtensions() {
		assertThat(this.loader.getFileExtensions())
				.isEqualTo(new String[] { "properties", "xml" });
	}

	@Test
	public void loadProperties() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.properties",
				new ClassPathResource("test-properties.properties", getClass()));
		PropertySource<?> source = loaded.get(0);
		assertThat(source.getProperty("test")).isEqualTo("properties");
	}

	@Test
	public void loadXml() throws Exception {
		List<PropertySource<?>> loaded = this.loader.load("test.xml",
				new ClassPathResource("test-xml.xml", getClass()));
		PropertySource<?> source = loaded.get(0);
		assertThat(source.getProperty("test")).isEqualTo("xml");
	}

}
