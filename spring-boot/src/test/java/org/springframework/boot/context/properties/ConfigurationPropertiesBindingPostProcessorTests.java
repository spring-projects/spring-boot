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

package org.springframework.boot.context.properties;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ConfigurationPropertiesBindingPostProcessor}.
 * 
 * @author Christian Dupuis
 * @author Phillip Webb
 */
public class ConfigurationPropertiesBindingPostProcessorTests {

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
		EnvironmentTestUtils.addEnvironment(this.context, "test.foo:spam");
		this.context.register(TestConfigurationWithValidatingSetter.class);
		try {
			this.context.refresh();
			fail("Expected exception");
		}
		catch (BeanCreationException ex) {
			BindException bex = (BindException) ex.getRootCause();
			assertEquals(1, bex.getErrorCount());
		}
	}

	@Test
	public void testValidationWithoutJSR303() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfigurationWithoutJSR303.class);
		try {
			this.context.refresh();
			fail("Expected exception");
		}
		catch (BeanCreationException ex) {
			BindException bex = (BindException) ex.getRootCause();
			assertEquals(1, bex.getErrorCount());
		}
	}

	@Test
	public void testValidationWithJSR303() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfigurationWithJSR303.class);
		try {
			this.context.refresh();
			fail("Expected exception");
		}
		catch (BeanCreationException ex) {
			BindException bex = (BindException) ex.getRootCause();
			assertEquals(2, bex.getErrorCount());
		}
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
	public void testInitializersSeeBoundProperties() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("bar", "foo");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfigurationWithInitializer.class);
		this.context.refresh();
	}

	@Test
	public void testPropertyWithEnum() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "test.value:foo");
		this.context.register(PropertyWithEnum.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithEnum.class).getValue(),
				equalTo(FooEnum.FOO));
	}

	@Test
	public void testValueBindingForDefaults() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "default.value:foo");
		this.context.register(PropertyWithValue.class);
		this.context.refresh();
		assertThat(this.context.getBean(PropertyWithValue.class).getValue(),
				equalTo("foo"));
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
			assertNotNull(this.bar);
		}

	}

	@ConfigurationProperties(prefix = "test")
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
	@ConfigurationProperties(prefix = "test")
	public static class PropertyWithEnum {

		private FooEnum value;

		public void setValue(FooEnum value) {
			this.value = value;
		}

		public FooEnum getValue() {
			return this.value;
		}

	}

	static enum FooEnum {
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

}
