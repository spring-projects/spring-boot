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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.validation.BindException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnableConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class EnableConfigurationPropertiesTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void close() {
		System.clearProperty("name");
		System.clearProperty("nested.name");
		System.clearProperty("nested_name");
	}

	@Test
	public void testBasicPropertiesBinding() {
		this.context.register(TestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(TestProperties.class)).hasSize(1);
		assertThat(this.context.containsBean(TestProperties.class.getName())).isTrue();
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testSystemPropertiesBinding() {
		this.context.register(TestConfiguration.class);
		System.setProperty("name", "foo");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(TestProperties.class)).hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testNestedSystemPropertiesBinding() {
		this.context.register(NestedConfiguration.class);
		System.setProperty("name", "foo");
		System.setProperty("nested.name", "bar");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void testNestedSystemPropertiesBindingWithUnderscore() {
		this.context.register(NestedConfiguration.class);
		System.setProperty("name", "foo");
		System.setProperty("nested_name", "bar");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void testNestedOsEnvironmentVariableWithUnderscore() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"NAME=foo", "NESTED_NAME=bar");
		this.context.register(NestedConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void testStrictPropertiesBinding() {
		removeSystemProperties();
		this.context.register(StrictTestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(StrictTestProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testPropertiesEmbeddedBinding() {
		this.context.register(EmbeddedTestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"spring_foo_name=foo");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(EmbeddedTestProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testOsEnvironmentVariableEmbeddedBinding() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"SPRING_FOO_NAME=foo");
		this.context.register(EmbeddedTestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(EmbeddedTestProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testIgnoreNestedPropertiesBinding() {
		removeSystemProperties();
		this.context.register(IgnoreNestedTestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo", "nested.name=bar");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(IgnoreNestedTestProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testExceptionOnValidation() {
		this.context.register(ExceptionIfInvalidTestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name:foo");
		this.thrown.expectCause(Matchers.<Throwable>instanceOf(BindException.class));
		this.context.refresh();
	}

	@Test
	public void testNoExceptionOnValidation() {
		this.context.register(NoExceptionIfInvalidTestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo");
		this.context.refresh();
		assertThat(this.context
				.getBeanNamesForType(NoExceptionIfInvalidTestProperties.class))
						.hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testNestedPropertiesBinding() {
		this.context.register(NestedConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo", "nested.name=bar");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void testBasicPropertiesBindingWithAnnotationOnBaseClass() {
		this.context.register(DerivedConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(DerivedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BaseProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testArrayPropertiesBinding() {
		this.context.register(TestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo", "array=1,2,3");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(TestProperties.class)).hasSize(1);
		assertThat(this.context.getBean(TestProperties.class).getArray()).hasSize(3);
	}

	@Test
	public void testCollectionPropertiesBindingFromYamlArray() {
		this.context.register(TestConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo", "list[0]=1", "list[1]=2");
		this.context.refresh();
		assertThat(this.context.getBean(TestProperties.class).getList()).hasSize(2);
	}

	@Test
	public void testPropertiesBindingWithoutAnnotation() {
		this.context.register(InvalidConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name:foo");

		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("No ConfigurationProperties annotation found");
		this.context.refresh();
	}

	@Test
	public void testPropertiesBindingWithoutAnnotationValue() {
		this.context.register(MoreConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(MoreProperties.class)).hasSize(1);
		assertThat(this.context.getBean(MoreProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void testPropertiesBindingWithDefaultsInXml() {
		this.context.register(TestConfiguration.class, DefaultXmlConfiguration.class);
		this.context.refresh();
		String[] beanNames = this.context.getBeanNamesForType(TestProperties.class);
		assertThat(beanNames).as("Wrong beans").containsExactly(beanNames);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("bar");
	}

	@Test
	public void testPropertiesBindingWithDefaultsInBeanMethod() {
		this.context.register(DefaultConfiguration.class);
		this.context.refresh();
		String[] beanNames = this.context.getBeanNamesForType(TestProperties.class);
		assertThat(beanNames).as("Wrong beans").containsExactly(beanNames);
		assertThat(this.context.getBean(TestProperties.class).name).isEqualTo("bar");
	}

	@Test
	public void testBindingDirectlyToFile() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ResourceBindingProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(ResourceBindingProperties.class).name)
				.isEqualTo("foo");
	}

	@Test
	public void testBindingDirectlyToFileResolvedFromEnvironment() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"binding.location=classpath:other.yml");
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ResourceBindingProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(ResourceBindingProperties.class).name)
				.isEqualTo("other");
	}

	@Test
	public void testBindingDirectlyToFileWithDefaultsWhenProfileNotFound() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.getEnvironment().addActiveProfile("nonexistent");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ResourceBindingProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(ResourceBindingProperties.class).name)
				.isEqualTo("foo");
	}

	@Test
	public void testBindingDirectlyToFileWithExplicitSpringProfile() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.getEnvironment().addActiveProfile("super");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ResourceBindingProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(ResourceBindingProperties.class).name)
				.isEqualTo("bar");
	}

	@Test
	public void testBindingDirectlyToFileWithTwoExplicitSpringProfiles() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.getEnvironment().setActiveProfiles("super", "other");
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ResourceBindingProperties.class))
				.hasSize(1);
		assertThat(this.context.getBean(ResourceBindingProperties.class).name)
				.isEqualTo("spam");
	}

	@Test
	public void testBindingWithTwoBeans() {
		this.context.register(MoreConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(TestProperties.class).length)
				.isEqualTo(1);
		assertThat(this.context.getBeanNamesForType(MoreProperties.class).length)
				.isEqualTo(1);
	}

	@Test
	public void testBindingWithParentContext() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(TestConfiguration.class);
		parent.refresh();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"name=foo");
		this.context.setParent(parent);
		this.context.register(TestConfiguration.class, TestConsumer.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(TestProperties.class).length)
				.isEqualTo(1);
		assertThat(parent.getBeanNamesForType(TestProperties.class).length).isEqualTo(1);
		assertThat(this.context.getBean(TestConsumer.class).getName()).isEqualTo("foo");
	}

	@Test
	public void testBindingOnlyParentContext() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(parent, "name=foo");
		parent.register(TestConfiguration.class);
		parent.refresh();
		this.context.setParent(parent);
		this.context.register(TestConsumer.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(TestProperties.class).length)
				.isEqualTo(0);
		assertThat(parent.getBeanNamesForType(TestProperties.class).length).isEqualTo(1);
		assertThat(this.context.getBean(TestConsumer.class).getName()).isEqualTo("foo");
	}

	@Test
	public void testUnderscoresInPrefix() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"spring_test_external_val=baz");
		this.context.register(SystemExampleConfig.class);
		this.context.refresh();
		assertThat(this.context.getBean(SystemEnvVar.class).getVal()).isEqualTo("baz");
	}

	@Test
	public void testSimpleAutoConfig() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"external.name=foo");
		this.context.register(ExampleConfig.class);
		this.context.refresh();
		assertThat(this.context.getBean(External.class).getName()).isEqualTo("foo");
	}

	@Test
	public void testExplicitType() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"external.name=foo");
		this.context.register(AnotherExampleConfig.class);
		this.context.refresh();
		assertThat(this.context.containsBean("external-" + External.class.getName()))
				.isTrue();
		assertThat(this.context.getBean(External.class).getName()).isEqualTo("foo");
	}

	@Test
	public void testMultipleExplicitTypes() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"external.name=foo", "another.name=bar");
		this.context.register(FurtherExampleConfig.class);
		this.context.refresh();
		assertThat(this.context.getBean(External.class).getName()).isEqualTo("foo");
		assertThat(this.context.getBean(Another.class).getName()).isEqualTo("bar");
	}

	@Test
	public void testBindingWithMapKeyWithPeriod() {
		this.context.register(ResourceBindingPropertiesWithMap.class);
		this.context.refresh();

		ResourceBindingPropertiesWithMap bean = this.context
				.getBean(ResourceBindingPropertiesWithMap.class);
		assertThat(bean.mymap.get("key3")).isEqualTo("value3");
		// this should not fail!!!
		// mymap looks to contain - {key1=, key3=value3}
		assertThat(bean.mymap.get("key1.key2")).isEqualTo("value12");
	}

	@Test
	public void testAnnotatedBean() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"external.name=bar", "spam.name=foo");
		this.context.register(TestConfigurationWithAnnotatedBean.class);
		this.context.refresh();
		assertThat(this.context.getBean(External.class).getName()).isEqualTo("foo");
	}

	/**
	 * Strict tests need a known set of properties so we remove system items which may be
	 * environment specific.
	 */
	private void removeSystemProperties() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.remove("systemProperties");
		sources.remove("systemEnvironment");
	}

	@Configuration
	@EnableConfigurationProperties
	public static class TestConfigurationWithAnnotatedBean {

		@Bean
		@ConfigurationProperties(prefix = "spam")
		public External testProperties() {
			return new External();
		}

	}

	@Configuration
	@EnableConfigurationProperties(TestProperties.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(StrictTestProperties.class)
	protected static class StrictTestConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(EmbeddedTestProperties.class)
	protected static class EmbeddedTestConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(IgnoreNestedTestProperties.class)
	protected static class IgnoreNestedTestConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(ExceptionIfInvalidTestProperties.class)
	protected static class ExceptionIfInvalidTestConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(NoExceptionIfInvalidTestProperties.class)
	protected static class NoExceptionIfInvalidTestConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(DerivedProperties.class)
	protected static class DerivedConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(NestedProperties.class)
	protected static class NestedConfiguration {

	}

	@Configuration
	protected static class DefaultConfiguration {

		@Bean
		public TestProperties testProperties() {
			TestProperties test = new TestProperties();
			test.setName("bar");
			return test;
		}

	}

	@Configuration
	@ImportResource("org/springframework/boot/context/properties/testProperties.xml")
	protected static class DefaultXmlConfiguration {

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

	@EnableConfigurationProperties({ SystemEnvVar.class })
	@Configuration
	public static class SystemExampleConfig {

	}

	@ConfigurationProperties(prefix = "external")
	public static class External {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties(prefix = "another")
	public static class Another {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties(prefix = "spring_test_external")
	public static class SystemEnvVar {

		private String val;

		public String getVal() {
			return this.val;
		}

		public void setVal(String val) {
			this.val = val;
		}

	}

	@Component
	protected static class TestConsumer {

		@Autowired
		private TestProperties properties;

		@PostConstruct
		public void init() {
			assertThat(this.properties).isNotNull();
		}

		public String getName() {
			return this.properties.name;
		}

	}

	@Configuration
	@EnableConfigurationProperties(MoreProperties.class)
	protected static class MoreConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(InvalidConfiguration.class)
	protected static class InvalidConfiguration {

	}

	@ConfigurationProperties
	protected static class NestedProperties {

		private String name;

		private final Nested nested = new Nested();

		public void setName(String name) {
			this.name = name;
		}

		public Nested getNested() {
			return this.nested;
		}

		protected static class Nested {

			private String name;

			public void setName(String name) {
				this.name = name;
			}

		}

	}

	@ConfigurationProperties
	protected static class BaseProperties {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

	}

	protected static class DerivedProperties extends BaseProperties {

	}

	@ConfigurationProperties
	protected static class TestProperties {

		private String name;

		private int[] array;

		private final List<Integer> list = new ArrayList<Integer>();

		// No getter - you should be able to bind to a write-only bean

		public void setName(String name) {
			this.name = name;
		}

		public void setArray(int... values) {
			this.array = values;
		}

		public int[] getArray() {
			return this.array;
		}

		public List<Integer> getList() {
			return this.list;
		}

	}

	@ConfigurationProperties(ignoreUnknownFields = false)
	protected static class StrictTestProperties extends TestProperties {

	}

	@ConfigurationProperties(prefix = "spring.foo")
	protected static class EmbeddedTestProperties extends TestProperties {

	}

	@ConfigurationProperties(ignoreUnknownFields = false, ignoreNestedProperties = true)
	protected static class IgnoreNestedTestProperties extends TestProperties {

	}

	@ConfigurationProperties
	protected static class ExceptionIfInvalidTestProperties extends TestProperties {

		@NotNull
		private String description;

		public String getDescription() {
			return this.description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

	@ConfigurationProperties(exceptionIfInvalid = false)
	protected static class NoExceptionIfInvalidTestProperties extends TestProperties {

		@NotNull
		private String description;

		public String getDescription() {
			return this.description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

	@ConfigurationProperties
	protected static class MoreProperties {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean
	}

	// No annotation
	protected static class InvalidProperties {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties(locations = "${binding.location:classpath:name.yml}")
	protected static class ResourceBindingProperties {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean
	}

	@EnableConfigurationProperties
	@ConfigurationProperties(locations = "${binding.location:classpath:map.yml}")
	protected static class ResourceBindingPropertiesWithMap {

		private Map<String, String> mymap;

		public void setMymap(Map<String, String> mymap) {
			this.mymap = mymap;
		}

		public Map<String, String> getMymap() {
			return this.mymap;
		}

	}

}
