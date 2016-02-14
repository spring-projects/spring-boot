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

package org.springframework.boot.bind;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.PropertySourcesBindingTests.TestConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link PropertySourcesPropertyValues} binding.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfig.class)
@IntegrationTest
public class PropertySourcesBindingTests {

	@Value("${foo:}")
	private String foo;

	@Autowired
	private Wrapper properties;

	@Test
	public void overridingOfPropertiesOrderOfAtPropertySources() {
		assertThat(this.properties.getBar(), is("override"));
	}

	@Test
	public void overridingOfPropertiesOrderOfAtPropertySourcesWherePropertyIsCapitalized() {
		assertThat(this.properties.getSpam(), is("BUCKET"));
	}

	@Test
	public void overridingOfPropertiesOrderOfAtPropertySourcesWherePropertyNamesDiffer() {
		assertThat(this.properties.getTheName(), is("NAME"));
	}

	@Test
	public void overridingOfPropertiesAndBindToAtValue() {
		assertThat(this.foo, is(this.properties.getFoo()));
	}

	@Test
	public void overridingOfPropertiesOrderOfApplicationProperties() {
		assertThat(this.properties.getFoo(), is("bucket"));
	}

	@Import({ SomeConfig.class })
	@PropertySources({ @PropertySource("classpath:/some.properties"),
			@PropertySource("classpath:/override.properties") })
	@Configuration
	@EnableConfigurationProperties(Wrapper.class)
	public static class TestConfig {

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration
	@PropertySources({ @PropertySource("classpath:/override.properties"),
			@PropertySource("classpath:/some.properties") })
	public static class SomeConfig {
	}

	@ConfigurationProperties
	public static class Wrapper {
		private String foo;

		private String bar;

		private String spam;

		private String theName;

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getSpam() {
			return this.spam;
		}

		public void setSpam(String spam) {
			this.spam = spam;
		}

		public String getTheName() {
			return this.theName;
		}

		public void setTheName(String theName) {
			this.theName = theName;
		}
	}

}
