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

package org.springframework.boot.autoconfigure;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringBootTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link PropertyPlaceholderAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class PropertyPlaceholderAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void propertyPlaceholderse() throws Exception {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				PlaceholderConfig.class);
		SpringBootTestUtils.addEnvironment(this.context, "foo:two");
		this.context.refresh();
		assertEquals("two", this.context.getBean(PlaceholderConfig.class).getFoo());
	}

	@Test
	public void propertyPlaceholdersOverride() throws Exception {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				PlaceholderConfig.class, PlaceholdersOverride.class);
		SpringBootTestUtils.addEnvironment(this.context, "foo:two");
		this.context.refresh();
		assertEquals("spam", this.context.getBean(PlaceholderConfig.class).getFoo());
	}

	@Configuration
	static class PlaceholderConfig {

		@Value("${foo:bar}")
		private String foo;

		public String getFoo() {
			return this.foo;
		}

	}

	@Configuration
	static class PlaceholdersOverride {

		@Bean
		public static PropertySourcesPlaceholderConfigurer morePlaceholders() {
			PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
			configurer.setProperties(StringUtils.splitArrayElementsIntoProperties(
					new String[] { "foo=spam" }, "="));
			configurer.setLocalOverride(true);
			configurer.setOrder(0);
			return configurer;
		}

	}

}
