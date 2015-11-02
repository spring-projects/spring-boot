/*
 * Copyright 2012-2015 the original author or authors.
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
import org.springframework.boot.bind.ConverterBindingTests.TestConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConfigurationProperties} binding with custom converters.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfig.class)
@IntegrationTest("foo=bar")
public class ConverterBindingTests {

	@Value("${foo:}")
	private String foo;

	@Autowired
	private Wrapper properties;

	@Test
	public void overridingOfPropertiesOrderOfAtPropertySources() {
		assertThat(this.properties.getFoo().getName(), is(this.foo));
	}

	@Configuration
	@EnableConfigurationProperties(Wrapper.class)
	public static class TestConfig {

		@Bean
		@ConfigurationPropertiesBinding
		public Converter<String, Foo> converter() {
			return new Converter<String, ConverterBindingTests.Foo>() {

				@Override
				public Foo convert(String source) {
					Foo foo = new Foo();
					foo.setName(source);
					return foo;
				}
			};
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	public static class Foo {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties
	public static class Wrapper {

		private Foo foo;

		public Foo getFoo() {
			return this.foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}

	}

}
