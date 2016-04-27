/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.ConverterBindingTests.TestConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationProperties} binding with custom converters.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@ContextConfiguration(classes = TestConfig.class, loader = SpringApplicationBindContextLoader.class)
@TestPropertySource(properties = { "foo=one", "bar=two" })
public class ConverterBindingTests {

	@Value("${foo:}")
	private String foo;

	@Value("${bar:}")
	private String bar;

	@Autowired
	private Wrapper properties;

	@Test
	public void overridingOfPropertiesOrderOfAtPropertySources() {
		assertThat(this.properties.getFoo().name).isEqualTo(this.foo);
		assertThat(this.properties.getBar().name).isEqualTo(this.bar);
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
					return new Foo(source);
				}
			};
		}

		@Bean
		public GenericConverter genericConverter() {
			return new GenericConverter() {
				@Override
				public Set<ConvertiblePair> getConvertibleTypes() {
					return Collections
							.singleton(new ConvertiblePair(String.class, Bar.class));
				}

				@Override
				public Object convert(Object source, TypeDescriptor sourceType,
						TypeDescriptor targetType) {
					return new Bar((String) source);
				}
			};
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	public static class Foo {

		private final String name;

		public Foo(String name) {
			this.name = name;
		}

	}

	public static class Bar {

		private final String name;

		public Bar(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties
	public static class Wrapper {

		private Foo foo;

		private Bar bar;

		public Foo getFoo() {
			return this.foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}

		public Bar getBar() {
			return this.bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

	}

}
