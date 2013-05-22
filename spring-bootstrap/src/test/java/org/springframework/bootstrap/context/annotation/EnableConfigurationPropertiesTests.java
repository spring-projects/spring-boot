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
package org.springframework.bootstrap.context.annotation;

import java.util.Collections;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 * 
 */
public class EnableConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testBasicPropertiesBinding() {
		this.context.register(TestConfiguration.class);
		this.context
				.getEnvironment()
				.getPropertySources()
				.addFirst(
						new MapPropertySource("test", Collections
								.<String, Object> singletonMap("name", "foo")));
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals("foo", this.context.getBean(TestProperties.class).getName());
	}

	@Test
	public void testPropertiesBindingWithoutAnnotation() {
		this.context.register(MoreConfiguration.class);
		this.context
				.getEnvironment()
				.getPropertySources()
				.addFirst(
						new MapPropertySource("test", Collections
								.<String, Object> singletonMap("name", "foo")));
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(MoreProperties.class).length);
		assertEquals("foo", this.context.getBean(MoreProperties.class).getName());
	}

	@Test
	public void testBindingWithTwoBeans() {
		this.context.register(MoreConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals(1, this.context.getBeanNamesForType(MoreProperties.class).length);
	}

	@Configuration
	@EnableConfigurationProperties(TestProperties.class)
	protected static class TestConfiguration {
	}

	@Configuration
	@EnableConfigurationProperties(MoreProperties.class)
	protected static class MoreConfiguration {
	}

	@ConfigurationProperties
	protected static class TestProperties {
		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	protected static class MoreProperties {
		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
