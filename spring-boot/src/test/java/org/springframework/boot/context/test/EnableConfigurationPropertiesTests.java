/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.test;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link EnableConfigurationProperties}.
 * 
 * @author Dave Syer
 */
public class EnableConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void open() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.getEnvironment()
				.getPropertySources()
				.addFirst(
						new PropertiesPropertySource("props",
								getProperties("external.name=foo\nanother.name=bar")));
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testSimpleAutoConfig() throws Exception {
		this.context.register(ExampleConfig.class);
		this.context.refresh();
		assertEquals("foo", this.context.getBean(External.class).getName());
	}

	@Test
	public void testExplicitType() throws Exception {
		this.context.register(AnotherExampleConfig.class);
		this.context.refresh();
		assertEquals("foo", this.context.getBean(External.class).getName());
	}

	@Test
	public void testMultipleExplicitTypes() throws Exception {
		this.context.register(FurtherExampleConfig.class);
		this.context.refresh();
		assertEquals("foo", this.context.getBean(External.class).getName());
		assertEquals("bar", this.context.getBean(Another.class).getName());
	}

	private Properties getProperties(String values) throws Exception {
		return PropertiesLoaderUtils.loadProperties(new ByteArrayResource(values
				.getBytes()));
	}

	@EnableConfigurationProperties
	@Configuration
	public static class ExampleConfig {
		@Bean
		public External external() {
			return new External();
		}
	}

	@EnableConfigurationProperties(External.class)
	@Configuration
	public static class AnotherExampleConfig {
	}

	@EnableConfigurationProperties({ External.class, Another.class })
	@Configuration
	public static class FurtherExampleConfig {
	}

	@ConfigurationProperties(name = "external")
	public static class External {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@ConfigurationProperties(name = "another")
	public static class Another {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
