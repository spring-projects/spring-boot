/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for {@link ConfigurationPropertiesBindingPostProcessor}.
 *
 * @author Christian Dupuis
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class ConfigurationPropertiesBindingPostProcessorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture output = new OutputCapture();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void bindToInterfaceBean() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("test.foo", "bar");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfigurationWithValidationAndInterface.class);
		this.context.refresh();
		assertThat(this.context.getBean(ValidatedPropertiesImpl.class).getFoo())
				.isEqualTo("bar");
	}

	@Test
	public void initializerSeeBoundProperties() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("bar", "foo");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfigurationWithInitializer.class);
		this.context.refresh();
	}

	@Test
	public void bindWithValueDefault() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"default.value=foo");
		this.context.register(PropertyWithValue.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithValue.class).getValue())
				.isEqualTo("foo");
	}

	@Test
	public void binderShouldNotInitializeFactoryBeans() throws Exception {
		ConfigurationPropertiesWithFactoryBean.factoryBeanInit = false;
		this.context = new AnnotationConfigApplicationContext() {
			@Override
			protected void onRefresh() throws BeansException {
				assertThat(ConfigurationPropertiesWithFactoryBean.factoryBeanInit)
						.as("Init too early").isFalse();
				super.onRefresh();
			}
		};
		this.context.register(ConfigurationPropertiesWithFactoryBean.class);
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(FactoryBeanTester.class);
		beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		this.context.registerBeanDefinition("test", beanDefinition);
		this.context.refresh();
		assertThat(ConfigurationPropertiesWithFactoryBean.factoryBeanInit).as("No init")
				.isTrue();
	}

	@Test
	public void bindWithoutConfigurationPropertiesAnnotation() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name:foo");
		this.context.register(ConfigurationPropertiesWithoutAnnotation.class);

		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("No ConfigurationProperties annotation found");
		this.context.refresh();
	}

	@Test
	public void multiplePropertySourcesPlaceholderConfigurer() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MultiplePropertySourcesPlaceholderConfigurer.class);
		this.context.refresh();
		assertThat(this.output.toString()).contains(
				"Multiple PropertySourcesPlaceholderConfigurer beans registered");
	}

	@Test
	public void overridingPropertiesWithPlaceholderResolutionInEnvShouldOverride()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		ConfigurableEnvironment env = this.context.getEnvironment();
		MutablePropertySources propertySources = env.getPropertySources();
		propertySources.addFirst(new SystemEnvironmentPropertySource("system",
				Collections.singletonMap("COM_EXAMPLE_BAR", "10")));
		Map<String, Object> source = new HashMap<>();
		source.put("com.example.bar", 5);
		source.put("com.example.foo", "${com.example.bar}");
		propertySources.addLast(new MapPropertySource("test", source));
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		int foo = this.context.getBean(TestConfiguration.class).getFoo();
		assertThat(foo).isEqualTo(10);
	}

	@Test
	public void unboundElementsFromSystemEnvironmentShouldNotThrowException()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		ConfigurableEnvironment env = this.context.getEnvironment();
		MutablePropertySources propertySources = env.getPropertySources();
		propertySources.addFirst(new MapPropertySource("test",
				Collections.singletonMap("com.example.foo", 5)));
		propertySources.addLast(new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("COM_EXAMPLE_OTHER", "10")));
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		int foo = this.context.getBean(TestConfiguration.class).getFoo();
		assertThat(foo).isEqualTo(5);
	}

	@Test
	public void rebindableConfigurationProperties() throws Exception {
		// gh-9160
		this.context = new AnnotationConfigApplicationContext();
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(PrototypePropertiesConfig.class);
		this.context.refresh();
		PrototypeBean first = this.context.getBean(PrototypeBean.class);
		assertThat(first.getOne()).isEqualTo("foo");
		source.put("example.one", "bar");
		sources.addFirst(new MapPropertySource("extra",
				Collections.singletonMap("example.two", "baz")));
		PrototypeBean second = this.context.getBean(PrototypeBean.class);
		assertThat(second.getOne()).isEqualTo("bar");
		assertThat(second.getTwo()).isEqualTo("baz");
	}

	@Test
	public void rebindableConfigurationPropertiesWithPropertySourcesPlaceholderConfigurer()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("example.one", "foo");
		sources.addFirst(new MapPropertySource("test-source", source));
		this.context.register(PrototypePropertiesConfig.class);
		this.context.register(PropertySourcesPlaceholderConfigurerConfiguration.class);
		this.context.refresh();
		PrototypeBean first = this.context.getBean(PrototypeBean.class);
		assertThat(first.getOne()).isEqualTo("foo");
		source.put("example.one", "bar");
		sources.addFirst(new MapPropertySource("extra",
				Collections.singletonMap("example.two", "baz")));
		PrototypeBean second = this.context.getBean(PrototypeBean.class);
		assertThat(second.getOne()).isEqualTo("bar");
		assertThat(second.getTwo()).isEqualTo("baz");
	}

	@Test
	public void converterIsFound() {
		prepareConverterContext(ConverterConfiguration.class, PersonProperty.class);
		this.context.refresh();
		Person person = this.context.getBean(PersonProperty.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	public void converterWithoutQualifierIsNotInvoked() {
		prepareConverterContext(NonQualifiedConverterConfiguration.class,
				PersonProperty.class);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectCause(instanceOf(BindException.class));
		this.context.refresh();
	}

	@Test
	public void genericConverterIsFound() {
		prepareConverterContext(GenericConverterConfiguration.class,
				PersonProperty.class);
		this.context.refresh();
		Person person = this.context.getBean(PersonProperty.class).getPerson();
		assertThat(person.firstName).isEqualTo("John");
		assertThat(person.lastName).isEqualTo("Smith");
	}

	@Test
	public void genericConverterWithoutQualifierIsNotInvoked() {
		prepareConverterContext(NonQualifiedGenericConverterConfiguration.class,
				PersonProperty.class);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectCause(instanceOf(BindException.class));
		this.context.refresh();
	}

	private void prepareConverterContext(Class<?>... config) {
		this.context = new AnnotationConfigApplicationContext();
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.addFirst(new MapPropertySource("test",
				Collections.singletonMap("test.person", "John Smith")));
		this.context.register(config);
	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties
	public static class TestConfigurationWithInitializer {

		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		@PostConstruct
		public void init() {
			assertThat(this.bar).isNotNull();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "com.example", ignoreUnknownFields = false)
	public static class TestConfiguration {

		private int foo;

		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		public int getFoo() {
			return this.foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}
	}

	@Configuration
	@EnableConfigurationProperties
	public static class TestConfigurationWithValidationAndInterface {

		@Bean
		public ValidatedPropertiesImpl testProperties() {
			return new ValidatedPropertiesImpl();
		}

	}

	interface ValidatedProperties {

		String getFoo();
	}

	@ConfigurationProperties("test")
	@Validated
	public static class ValidatedPropertiesImpl implements ValidatedProperties {

		@NotNull
		private String foo;

		@Override
		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	@Validated
	public static class PropertyWithValue {

		@Value("${default.value}")
		private String value;

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class PrototypePropertiesConfig {

		@Bean
		@Scope("prototype")
		@ConfigurationProperties("example")
		public PrototypeBean prototypeBean() {
			return new PrototypeBean();
		}

	}

	@Configuration
	public static class PropertySourcesPlaceholderConfigurerConfiguration {

		@Bean
		public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	public static class PrototypeBean {

		private String one;

		private String two;

		public String getOne() {
			return this.one;
		}

		public void setOne(String one) {
			this.one = one;
		}

		public String getTwo() {
			return this.two;
		}

		public void setTwo(String two) {
			this.two = two;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class ConfigurationPropertiesWithFactoryBean {

		public static boolean factoryBeanInit;

	}

	@SuppressWarnings("rawtypes")
	// Must be a raw type
	static class FactoryBeanTester implements FactoryBean, InitializingBean {

		@Override
		public Object getObject() throws Exception {
			return Object.class;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			ConfigurationPropertiesWithFactoryBean.factoryBeanInit = true;
		}

	}

	@Configuration
	@EnableConfigurationProperties(PropertyWithoutConfigurationPropertiesAnnotation.class)
	public static class ConfigurationPropertiesWithoutAnnotation {

	}

	@Configuration
	@EnableConfigurationProperties
	public static class MultiplePropertySourcesPlaceholderConfigurer {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer1() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer2() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	public static class PropertyWithoutConfigurationPropertiesAnnotation {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Configuration
	static class ConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	@Configuration
	static class NonQualifiedConverterConfiguration {

		@Bean
		public Converter<String, Person> personConverter() {
			return new PersonConverter();
		}

	}

	private static class PersonConverter implements Converter<String, Person> {

		@Nullable
		@Override
		public Person convert(String source) {
			String[] content = StringUtils.split(source, " ");
			return new Person(content[0], content[1]);
		}
	}

	@Configuration
	static class GenericConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	@Configuration
	static class NonQualifiedGenericConverterConfiguration {

		@Bean
		public GenericConverter genericPersonConverter() {
			return new GenericPersonConverter();
		}

	}

	private static class GenericPersonConverter implements GenericConverter {

		@Nullable
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Person.class));
		}

		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			String[] content = StringUtils.split((String) source, " ");
			return new Person(content[0], content[1]);
		}
	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	public static class PersonProperty {

		private Person person;

		public Person getPerson() {
			return this.person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

	}

	static class Person {

		private final String firstName;

		private final String lastName;

		Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

	}

}
