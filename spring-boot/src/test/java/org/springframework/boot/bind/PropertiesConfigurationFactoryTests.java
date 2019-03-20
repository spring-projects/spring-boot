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

package org.springframework.boot.bind;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import javax.validation.Validation;
import javax.validation.constraints.NotNull;

import org.junit.Test;

import org.springframework.beans.NotWritablePropertyException;
import org.springframework.boot.context.config.RandomValuePropertySource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.validation.BindException;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesConfigurationFactory}.
 *
 * @author Dave Syer
 */
public class PropertiesConfigurationFactoryTests {

	private PropertiesConfigurationFactory<Foo> factory;

	private Validator validator;

	private boolean ignoreUnknownFields = true;

	private String targetName = null;

	@Test
	public void testValidPropertiesLoadsWithDash() throws Exception {
		this.ignoreUnknownFields = false;
		Foo foo = createFoo("na-me: blah\nbar: blah");
		assertThat(foo.bar).isEqualTo("blah");
		assertThat(foo.name).isEqualTo("blah");
	}

	@Test
	public void testUnknownPropertyOkByDefault() throws Exception {
		Foo foo = createFoo("hi: hello\nname: foo\nbar: blah");
		assertThat(foo.bar).isEqualTo("blah");
	}

	@Test(expected = NotWritablePropertyException.class)
	public void testUnknownPropertyCausesLoadFailure() throws Exception {
		this.ignoreUnknownFields = false;
		createFoo("hi: hello\nname: foo\nbar: blah");
	}

	@Test(expected = BindException.class)
	public void testMissingPropertyCausesValidationError() throws Exception {
		this.validator = new SpringValidatorAdapter(
				Validation.buildDefaultValidatorFactory().getValidator());
		createFoo("bar: blah");
	}

	@Test
	@Deprecated
	public void testValidationErrorCanBeSuppressed() throws Exception {
		this.validator = new SpringValidatorAdapter(
				Validation.buildDefaultValidatorFactory().getValidator());
		setupFactory();
		this.factory.setExceptionIfInvalid(false);
		bindFoo("bar: blah");
	}

	@Test
	public void systemEnvironmentBindingFailuresAreIgnored() throws Exception {
		setupFactory();
		MutablePropertySources propertySources = new MutablePropertySources();
		MockPropertySource propertySource = new MockPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySource.setProperty("doesNotExist", "foo");
		propertySource.setProperty("name", "bar");
		propertySources.addFirst(propertySource);
		this.factory.setPropertySources(propertySources);
		this.factory.setIgnoreUnknownFields(false);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.name).isEqualTo("bar");
	}

	@Test
	public void systemEnvironmentBindingWithDefaults() throws Exception {
		setupFactory();
		MutablePropertySources propertySources = new MutablePropertySources();
		MockPropertySource propertySource = new MockPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySource.setProperty("name", "${foo.name:bar}");
		propertySources.addFirst(propertySource);
		this.factory.setPropertySources(propertySources);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.name).isEqualTo("bar");
	}

	@Test
	public void systemEnvironmentNoResolvePlaceholders() throws Exception {
		setupFactory();
		MutablePropertySources propertySources = new MutablePropertySources();
		MockPropertySource propertySource = new MockPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySource.setProperty("name", "${foo.name:bar}");
		propertySources.addFirst(propertySource);
		this.factory.setPropertySources(propertySources);
		this.factory.setResolvePlaceholders(false);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.name).isEqualTo("${foo.name:bar}");
	}

	@Test
	public void systemPropertyBindingFailuresAreIgnored() throws Exception {
		setupFactory();
		MutablePropertySources propertySources = new MutablePropertySources();
		MockPropertySource propertySource = new MockPropertySource(
				StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySource.setProperty("doesNotExist", "foo");
		propertySource.setProperty("name", "bar");
		propertySources.addFirst(propertySource);
		this.factory.setPropertySources(propertySources);
		this.factory.setIgnoreUnknownFields(false);
		this.factory.afterPropertiesSet();
	}

	@Test
	public void testBindWithDashPrefix() throws Exception {
		// gh-4045
		this.targetName = "foo-bar";
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new SystemEnvironmentPropertySource("systemEnvironment",
				Collections.<String, Object>singletonMap("FOO_BAR_NAME", "blah")));
		propertySources.addLast(new RandomValuePropertySource());
		setupFactory();
		this.factory.setPropertySources(propertySources);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.name).isEqualTo("blah");
	}

	@Test
	public void testBindWithDelimitedPrefixUsingMatchingDelimiter() throws Exception {
		this.targetName = "env_foo";
		this.ignoreUnknownFields = false;
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new SystemEnvironmentPropertySource("systemEnvironment",
				Collections.<String, Object>singletonMap("ENV_FOO_NAME", "blah")));
		propertySources.addLast(new RandomValuePropertySource("random"));
		setupFactory();
		this.factory.setPropertySources(propertySources);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.name).isEqualTo("blah");
	}

	@Test
	public void testBindWithDelimitedPrefixUsingDifferentDelimiter() throws Exception {
		this.targetName = "env.foo";
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new SystemEnvironmentPropertySource("systemEnvironment",
				Collections.<String, Object>singletonMap("ENV_FOO_NAME", "blah")));
		propertySources.addLast(new RandomValuePropertySource("random"));
		this.ignoreUnknownFields = false;
		setupFactory();
		this.factory.setPropertySources(propertySources);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.name).isEqualTo("blah");
	}

	@Test
	public void propertyWithAllUpperCaseSuffixCanBeBound() throws Exception {
		Foo foo = createFoo("foo-bar-u-r-i:baz");
		assertThat(foo.fooBarURI).isEqualTo("baz");
	}

	@Test
	public void propertyWithAllUpperCaseInTheMiddleCanBeBound() throws Exception {
		Foo foo = createFoo("foo-d-l-q-bar:baz");
		assertThat(foo.fooDLQBar).isEqualTo(("baz"));
	}

	@Test
	public void currentDirectoryCanBeBoundToFileProperty() throws Exception {
		PropertiesConfigurationFactory<FileProperties> factory = new PropertiesConfigurationFactory<FileProperties>(
				FileProperties.class);
		factory.setApplicationContext(new AnnotationConfigApplicationContext());
		factory.setConversionService(new DefaultConversionService());
		Properties properties = PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource("someFile: .".getBytes()));
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new PropertiesPropertySource("test", properties));
		factory.setPropertySources(propertySources);
		factory.afterPropertiesSet();
		FileProperties fileProperties = factory.getObject();
		assertThat(fileProperties.getSomeFile()).isEqualTo(new File("."));
	}

	private Foo createFoo(final String values) throws Exception {
		setupFactory();
		return bindFoo(values);
	}

	private Foo bindFoo(final String values) throws Exception {
		Properties properties = PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource(values.getBytes()));
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(new PropertiesPropertySource("test", properties));
		this.factory.setPropertySources(propertySources);
		this.factory.afterPropertiesSet();
		return this.factory.getObject();
	}

	private void setupFactory() throws IOException {
		this.factory = new PropertiesConfigurationFactory<Foo>(Foo.class);
		this.factory.setValidator(this.validator);
		this.factory.setTargetName(this.targetName);
		this.factory.setIgnoreUnknownFields(this.ignoreUnknownFields);
		this.factory.setMessageSource(new StaticMessageSource());
	}

	// Foo needs to be public and to have setters for all properties
	public static class Foo {

		@NotNull
		private String name;

		private String bar;

		private String spring_foo_baz;

		private String fooBar;

		private String fooBarURI;

		private String fooDLQBar;

		public String getSpringFooBaz() {
			return this.spring_foo_baz;
		}

		public void setSpringFooBaz(String spring_foo_baz) {
			this.spring_foo_baz = spring_foo_baz;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getFooBar() {
			return this.fooBar;
		}

		public void setFooBar(String fooBar) {
			this.fooBar = fooBar;
		}

		public String getFooBarURI() {
			return this.fooBarURI;
		}

		public void setFooBarURI(String fooBarURI) {
			this.fooBarURI = fooBarURI;
		}

		public String getFooDLQBar() {
			return this.fooDLQBar;
		}

		public void setFooDLQBar(String fooDLQBar) {
			this.fooDLQBar = fooDLQBar;
		}

	}

	public static class FileProperties {

		private File someFile;

		public File getSomeFile() {
			return this.someFile;
		}

		public void setSomeFile(File someFile) {
			this.someFile = someFile;
		}

	}

}
