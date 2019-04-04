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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link EndpointAutoConfiguration}.
 *
 * @author Chao Chang
 */
public class EndpointAutoConfigurationTests {

	private static final AutoConfigurations CONFIGURATIONS = AutoConfigurations
			.of(EndpointAutoConfiguration.class);

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(CONFIGURATIONS);

	@Test
	public void mapShouldUseConfigurationConverter() {
		this.contextRunner.withUserConfiguration(ConverterConfiguration.class)
				.run((context) -> {
					ParameterValueMapper parameterValueMapper = context
							.getBean(ParameterValueMapper.class);
					Object paramValue = parameterValueMapper.mapParameterValue(
							new TestOperationParameter(Person.class), "John Smith");
					assertThat(paramValue).isInstanceOf(Person.class);
					Person person = (Person) paramValue;
					assertThat(person.firstName).isEqualTo("John");
					assertThat(person.lastName).isEqualTo("Smith");
				});
	}

	@Test
	public void mapWhenConfigurationConverterIsNotQualifiedShouldNotConvert() {
		assertThatExceptionOfType(ParameterMappingException.class).isThrownBy(() -> {
			this.contextRunner
					.withUserConfiguration(NonQualifiedConverterConfiguration.class)
					.run((context) -> {
						ParameterValueMapper parameterValueMapper = context
								.getBean(ParameterValueMapper.class);
						parameterValueMapper.mapParameterValue(
								new TestOperationParameter(Person.class), "John Smith");
					});

		}).withCauseInstanceOf(ConverterNotFoundException.class);

	}

	@Test
	public void mapShouldUseGenericConfigurationConverter() {
		this.contextRunner.withUserConfiguration(GenericConverterConfiguration.class)
				.run((context) -> {
					ParameterValueMapper parameterValueMapper = context
							.getBean(ParameterValueMapper.class);
					Object paramValue = parameterValueMapper.mapParameterValue(
							new TestOperationParameter(Person.class), "John Smith");
					assertThat(paramValue).isInstanceOf(Person.class);
					Person person = (Person) paramValue;
					assertThat(person.firstName).isEqualTo("John");
					assertThat(person.lastName).isEqualTo("Smith");
				});
	}

	@Test
	public void mapWhenGenericConfigurationConverterIsNotQualifiedShouldNotConvert() {
		assertThatExceptionOfType(ParameterMappingException.class).isThrownBy(() -> {
			this.contextRunner
					.withUserConfiguration(
							NonQualifiedGenericConverterConfiguration.class)
					.run((context) -> {
						ParameterValueMapper parameterValueMapper = context
								.getBean(ParameterValueMapper.class);
						parameterValueMapper.mapParameterValue(
								new TestOperationParameter(Person.class), "John Smith");
					});

		}).withCauseInstanceOf(ConverterNotFoundException.class);

	}

	static class PersonConverter implements Converter<String, Person> {

		@Override
		public Person convert(String source) {
			String[] content = StringUtils.split(source, " ");
			return new Person(content[0], content[1]);
		}

	}

	static class GenericPersonConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Person.class));
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			String[] content = StringUtils.split((String) source, " ");
			return new Person(content[0], content[1]);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConverterConfiguration {

		@Bean
		@EndpointConverter
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonQualifiedConverterConfiguration {

		@Bean
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericConverterConfiguration {

		@Bean
		@EndpointConverter
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonQualifiedGenericConverterConfiguration {

		@Bean
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	static class Person {

		private final String firstName;

		private final String lastName;

		Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

	}

	private static class TestOperationParameter implements OperationParameter {

		private final Class<?> type;

		TestOperationParameter(Class<?> type) {
			this.type = type;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public Class<?> getType() {
			return this.type;
		}

		@Override
		public boolean isMandatory() {
			return false;
		}

	}

}
