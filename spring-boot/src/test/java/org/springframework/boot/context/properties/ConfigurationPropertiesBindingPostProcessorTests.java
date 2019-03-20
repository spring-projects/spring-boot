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

package org.springframework.boot.context.properties;

import java.util.List;
import java.util.Map;

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
import org.springframework.boot.bind.RelaxedBindingNotWritablePropertyException;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConfigurationPropertiesBindingPostProcessor}.
 *
 * @author Christian Dupuis
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ConfigurationPropertiesBindingPostProcessorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public InternalOutputCapture output = new InternalOutputCapture();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testValidationWithSetter() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.foo=spam");
		this.context.register(TestConfigurationWithValidatingSetter.class);
		assertBindingFailure(1);
	}

	@Test
	public void unknownFieldFailureMessageContainsDetailsOfPropertyOrigin() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"com.example.baz=spam");
		this.context.register(TestConfiguration.class);
		try {
			this.context.refresh();
			fail("Expected exception");
		}
		catch (BeanCreationException ex) {
			RelaxedBindingNotWritablePropertyException bex = (RelaxedBindingNotWritablePropertyException) ex
					.getRootCause();
			assertThat(bex.getMessage())
					.startsWith("Failed to bind 'com.example.baz' from '"
							+ TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME
							+ "' to 'baz' " + "property on '"
							+ TestConfiguration.class.getName());
		}
	}

	@Test
	public void testValidationWithoutJSR303() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfigurationWithoutJSR303.class);
		assertBindingFailure(1);
	}

	@Test
	public void testValidationWithJSR303() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfigurationWithJSR303.class);
		assertBindingFailure(2);
	}

	@Test
	public void testValidationAndNullOutValidator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		ConfigurationPropertiesBindingPostProcessor bean = this.context
				.getBean(ConfigurationPropertiesBindingPostProcessor.class);
		assertThat(ReflectionTestUtils.getField(bean, "validator")).isNull();
	}

	@Test
	public void testSuccessfulValidationWithJSR303() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("test.foo", "123456");
		env.setProperty("test.bar", "654321");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfigurationWithJSR303.class);
		this.context.refresh();
	}

	@Test
	public void testSuccessfulValidationWithInterface() {
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
	public void testInitializersSeeBoundProperties() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("bar", "foo");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfigurationWithInitializer.class);
		this.context.refresh();
	}

	@Test
	public void testValidationWithCustomValidator() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfigurationWithCustomValidator.class);
		assertBindingFailure(1);
	}

	@Test
	public void testValidationWithCustomValidatorNotSupported() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("test.foo", "bar");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfigurationWithCustomValidator.class,
				PropertyWithValidatingSetter.class);
		assertBindingFailure(1);
	}

	@Test
	public void testPropertyWithEnum() throws Exception {
		doEnumTest("test.theValue=foo");
	}

	@Test
	public void testRelaxedPropertyWithEnum() throws Exception {
		doEnumTest("test.the-value=FoO");
		doEnumTest("TEST_THE_VALUE=FoO");
		doEnumTest("test.THE_VALUE=FoO");
		doEnumTest("test_the_value=FoO");
	}

	private void doEnumTest(String property) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, property);
		this.context.register(PropertyWithEnum.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithEnum.class).getTheValue())
				.isEqualTo(FooEnum.FOO);
		this.context.close();
	}

	@Test
	public void testRelaxedPropertyWithSetOfEnum() {
		doEnumSetTest("test.the-values=foo,bar", FooEnum.FOO, FooEnum.BAR);
		doEnumSetTest("test.the-values=foo", FooEnum.FOO);
		doEnumSetTest("TEST_THE_VALUES=FoO", FooEnum.FOO);
		doEnumSetTest("test_the_values=BaR,FoO", FooEnum.BAR, FooEnum.FOO);
	}

	private void doEnumSetTest(String property, FooEnum... expected) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, property);
		this.context.register(PropertyWithEnum.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithEnum.class).getTheValues())
				.contains(expected);
		this.context.close();
	}

	@Test
	public void testValueBindingForDefaults() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"default.value=foo");
		this.context.register(PropertyWithValue.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithValue.class).getValue())
				.isEqualTo("foo");
	}

	@Test
	public void configurationPropertiesWithFactoryBean() throws Exception {
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
	public void configurationPropertiesWithCharArray() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.chars=word");
		this.context.register(PropertyWithCharArray.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithCharArray.class).getChars())
				.isEqualTo("word".toCharArray());
	}

	@Test
	public void configurationPropertiesWithArrayExpansion() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.chars[4]=s");
		this.context.register(PropertyWithCharArrayExpansion.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithCharArrayExpansion.class).getChars())
				.isEqualTo("words".toCharArray());
	}

	@Test
	public void notWritablePropertyException() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.madeup:word");
		this.context.register(PropertyWithCharArray.class);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("test");
		this.context.refresh();
	}

	@Test
	public void relaxedPropertyNamesSame() throws Exception {
		testRelaxedPropertyNames("test.FOO_BAR=test1", "test.FOO_BAR=test2",
				"test.BAR-B-A-Z=testa", "test.BAR-B-A-Z=testb");
	}

	@Test
	public void relaxedPropertyNamesMixed() throws Exception {
		testRelaxedPropertyNames("test.FOO_BAR=test2", "test.foo-bar=test1",
				"test.BAR-B-A-Z=testb", "test.bar_b_a_z=testa");
	}

	private void testRelaxedPropertyNames(String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				environment);
		this.context.register(RelaxedPropertyNames.class);
		this.context.refresh();
		RelaxedPropertyNames bean = this.context.getBean(RelaxedPropertyNames.class);
		assertThat(bean.getFooBar()).isEqualTo("test2");
		assertThat(bean.getBarBAZ()).isEqualTo("testb");
	}

	@Test
	public void nestedProperties() throws Exception {
		// gh-3539
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"TEST_NESTED_VALUE=test1");
		this.context.register(PropertyWithNestedValue.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithNestedValue.class).getNested()
				.getValue()).isEqualTo("test1");
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
	public void propertiesWithMap() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.map.foo=bar");
		this.context.register(PropertiesWithMap.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertiesWithMap.class).getMap())
				.containsEntry("foo", "bar");
	}

	private void assertBindingFailure(int errorCount) {
		try {
			this.context.refresh();
			fail("Expected exception");
		}
		catch (BeanCreationException ex) {
			BindException bex = (BindException) ex.getRootCause();
			assertThat(bex.getErrorCount()).isEqualTo(errorCount);
		}
	}

	@Test
	public void customProtocolResolverIsInvoked() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.resource=application.properties");
		ProtocolResolver protocolResolver = mock(ProtocolResolver.class);
		given(protocolResolver.resolve(anyString(), any(ResourceLoader.class)))
				.willReturn(null);
		this.context.addProtocolResolver(protocolResolver);
		this.context.register(PropertiesWithResource.class);
		this.context.refresh();
		verify(protocolResolver).resolve(eq("application.properties"),
				any(ResourceLoader.class));
	}

	@Test
	public void customProtocolResolver() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"test.resource=test:/application.properties");
		this.context.addProtocolResolver(new TestProtocolResolver());
		this.context.register(PropertiesWithResource.class);
		this.context.refresh();
		Resource resource = this.context.getBean(PropertiesWithResource.class)
				.getResource();
		assertThat(resource).isNotNull();
		assertThat(resource).isInstanceOf(ClassPathResource.class);
		assertThat(resource.exists()).isTrue();
		assertThat(((ClassPathResource) resource).getPath())
				.isEqualTo("application.properties");
	}

	@Configuration
	@EnableConfigurationProperties
	public static class TestConfigurationWithValidatingSetter {

		@Bean
		public PropertyWithValidatingSetter testProperties() {
			return new PropertyWithValidatingSetter();
		}

	}

	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithValidatingSetter {

		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
			if (!foo.equals("bar")) {
				throw new IllegalArgumentException("Wrong value for foo");
			}
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class TestConfigurationWithoutJSR303 {

		@Bean
		public PropertyWithoutJSR303 testProperties() {
			return new PropertyWithoutJSR303();
		}

	}

	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithoutJSR303 implements Validator {

		private String foo;

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.isAssignableFrom(getClass());
		}

		@Override
		public void validate(Object target, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "foo", "TEST1");
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class TestConfigurationWithJSR303 {

		@Bean
		public PropertyWithJSR303 testProperties() {
			return new PropertyWithJSR303();
		}

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

		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	public static class PropertyWithJSR303 extends PropertyWithoutJSR303 {

		@NotNull
		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
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
	public static class TestConfigurationWithCustomValidator {

		@Bean
		public PropertyWithCustomValidator propertyWithCustomValidator() {
			return new PropertyWithCustomValidator();
		}

		@Bean
		public Validator configurationPropertiesValidator() {
			return new CustomPropertyValidator();
		}

	}

	@ConfigurationProperties(prefix = "custom")
	public static class PropertyWithCustomValidator {

		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	public static class CustomPropertyValidator implements Validator {

		@Override
		public boolean supports(Class<?> aClass) {
			return aClass == PropertyWithCustomValidator.class;
		}

		@Override
		public void validate(Object o, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "foo", "TEST1");
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test", ignoreUnknownFields = false)
	public static class PropertyWithCharArray {

		private char[] chars;

		public char[] getChars() {
			return this.chars;
		}

		public void setChars(char[] chars) {
			this.chars = chars;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test", ignoreUnknownFields = false)
	public static class PropertyWithCharArrayExpansion {

		private char[] chars = new char[] { 'w', 'o', 'r', 'd' };

		public char[] getChars() {
			return this.chars;
		}

		public void setChars(char[] chars) {
			this.chars = chars;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithEnum {

		private FooEnum theValue;

		private List<FooEnum> theValues;

		public void setTheValue(FooEnum value) {
			this.theValue = value;
		}

		public FooEnum getTheValue() {
			return this.theValue;
		}

		public List<FooEnum> getTheValues() {
			return this.theValues;
		}

		public void setTheValues(List<FooEnum> theValues) {
			this.theValues = theValues;
		}

	}

	enum FooEnum {

		FOO, BAZ, BAR

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
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
	@ConfigurationProperties(prefix = "test")
	public static class PropertiesWithMap {

		@Bean
		public Validator validator() {
			return new LocalValidatorFactoryBean();
		}

		private Map<String, String> map;

		public Map<String, String> getMap() {
			return this.map;
		}

		public void setMap(Map<String, String> map) {
			this.map = map;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class ConfigurationPropertiesWithFactoryBean {

		public static boolean factoryBeanInit;

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	public static class RelaxedPropertyNames {

		private String fooBar;

		private String barBAZ;

		public String getFooBar() {
			return this.fooBar;
		}

		public void setFooBar(String fooBar) {
			this.fooBar = fooBar;
		}

		public String getBarBAZ() {
			return this.barBAZ;
		}

		public void setBarBAZ(String barBAZ) {
			this.barBAZ = barBAZ;
		}

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
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithNestedValue {

		private Nested nested = new Nested();

		public Nested getNested() {
			return this.nested;
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		public static class Nested {

			@Value("${default.value}")
			private String value;

			public void setValue(String value) {
				this.value = value;
			}

			public String getValue() {
				return this.value;
			}

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
	@EnableConfigurationProperties
	@ConfigurationProperties(prefix = "test")
	public static class PropertiesWithResource {

		private Resource resource;

		public Resource getResource() {
			return this.resource;
		}

		public void setResource(Resource resource) {
			this.resource = resource;
		}

	}

	private static class TestProtocolResolver implements ProtocolResolver {

		public static final String PREFIX = "test:/";

		@Override
		public Resource resolve(String location, ResourceLoader resourceLoader) {
			if (location.startsWith(PREFIX)) {
				String path = location.substring(PREFIX.length(), location.length());
				return new ClassPathResource(path);
			}
			return null;
		}

	}

}
