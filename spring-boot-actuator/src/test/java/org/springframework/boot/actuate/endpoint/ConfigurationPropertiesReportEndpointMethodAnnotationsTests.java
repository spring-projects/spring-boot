/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} when used with bean methods.
 *
 * @author Dave Syer
 */
public class ConfigurationPropertiesReportEndpointMethodAnnotationsTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNaming() throws Exception {
		this.context.register(Config.class);
		EnvironmentTestUtils.addEnvironment(this.context, "other.name:foo",
				"first.name:bar");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("other");
		assertNotNull(nestedProperties);
		assertEquals("other", nestedProperties.get("prefix"));
		assertNotNull(nestedProperties.get("properties"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOverride() throws Exception {
		this.context.register(Other.class);
		EnvironmentTestUtils.addEnvironment(this.context, "other.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("bar");
		assertNotNull(nestedProperties);
		assertEquals("other", nestedProperties.get("prefix"));
		assertNotNull(nestedProperties.get("properties"));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		@ConfigurationProperties(prefix = "first")
		public Foo foo() {
			return new Foo();
		}

		@Bean
		@ConfigurationProperties(prefix = "other")
		public Foo other() {
			return new Foo();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class Other {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		@ConfigurationProperties(prefix = "other")
		public Bar bar() {
			return new Bar();
		}

	}

	public static class Foo {

		private String name = "654321";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties(prefix = "test")
	public static class Bar {

		private String name = "654321";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
