/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

import org.junit.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link PropertiesPropertySourceLoader}.
 *
 * @author Phillip Webb
 */
public class PropertiesPropertySourceLoaderTests {

	private PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

	@Test
	public void getFileExtensions() throws Exception {
		assertThat(this.loader.getFileExtensions(), equalTo(new String[] { "properties",
				"xml" }));
	}

	@Test
	public void loadProperties() throws Exception {
		PropertySource<?> source = this.loader.load("test.properties",
				new ClassPathResource("test-properties.properties", getClass()), null);
		assertThat(source.getProperty("test"), equalTo((Object) "properties"));
	}

	@Test
	public void loadXml() throws Exception {
		PropertySource<?> source = this.loader.load("test.xml", new ClassPathResource(
				"test-xml.xml", getClass()), null);
		assertThat(source.getProperty("test"), equalTo((Object) "xml"));
	}

}
