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

package org.springframework.boot.bind;

import java.io.IOException;

import javax.validation.Validation;
import javax.validation.constraints.NotNull;

import org.junit.Test;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import static org.junit.Assert.assertEquals;

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
	public void testValidPropertiesLoadsWithNoErrors() throws Exception {
		Foo foo = createFoo("name: blah\nbar: blah");
		assertEquals("blah", foo.bar);
		assertEquals("blah", foo.name);
	}

	@Test
	public void testValidPropertiesLoadsWithUpperCase() throws Exception {
		Foo foo = createFoo("NAME: blah\nbar: blah");
		assertEquals("blah", foo.bar);
		assertEquals("blah", foo.name);
	}

	@Test
	public void testValidPropertiesLoadsWithDash() throws Exception {
		Foo foo = createFoo("na-me: blah\nbar: blah");
		assertEquals("blah", foo.bar);
		assertEquals("blah", foo.name);
	}

	
	@Test
	public void testUnderscore() throws Exception {
		Foo foo = createFoo("spring_foo_baz: blah\nname: blah");
		assertEquals("blah", foo.spring_foo_baz);
		assertEquals("blah", foo.name);
	}
	
	@Test
	public void testUnknownPropertyOkByDefault() throws Exception {
		Foo foo = createFoo("hi: hello\nname: foo\nbar: blah");
		assertEquals("blah", foo.bar);
	}

	@Test(expected = NotWritablePropertyException.class)
	public void testUnknownPropertyCausesLoadFailure() throws Exception {
		this.ignoreUnknownFields = false;
		createFoo("hi: hello\nname: foo\nbar: blah");
	}

	@Test(expected = BindException.class)
	public void testMissingPropertyCausesValidationError() throws Exception {
		this.validator = new SpringValidatorAdapter(Validation
				.buildDefaultValidatorFactory().getValidator());
		createFoo("bar: blah");
	}

	@Test
	public void testValidationErrorCanBeSuppressed() throws Exception {
		this.validator = new SpringValidatorAdapter(Validation
				.buildDefaultValidatorFactory().getValidator());
		setupFactory();
		this.factory.setExceptionIfInvalid(false);
		bindFoo("bar: blah");
	}

	@Test
	public void testBindToNamedTarget() throws Exception {
		this.targetName = "foo";
		Foo foo = createFoo("hi: hello\nfoo.name: foo\nfoo.bar: blah");
		assertEquals("blah", foo.bar);
	}

	private Foo createFoo(final String values) throws Exception {
		setupFactory();
		return bindFoo(values);
	}

	private Foo bindFoo(final String values) throws Exception {
		this.factory.setProperties(PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource(values.getBytes())));
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

		public String getSpringFooBaz() {
			return spring_foo_baz;
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

	}

}
