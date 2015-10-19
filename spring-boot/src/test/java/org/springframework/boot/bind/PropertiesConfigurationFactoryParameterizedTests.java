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

import java.io.IOException;
import java.util.Properties;

import javax.validation.constraints.NotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.junit.Assert.assertEquals;

/**
 * Parameterized tests for {@link PropertiesConfigurationFactory}
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class PropertiesConfigurationFactoryParameterizedTests {

	private final boolean usePropertySource;

	private String targetName;

	private PropertiesConfigurationFactory<Foo> factory = new PropertiesConfigurationFactory<Foo>(
			Foo.class);

	@Parameters
	public static Object[] parameters() {
		return new Object[] { new Object[] { false, false }, new Object[] { false, true },
				new Object[] { true, false }, new Object[] { true, true } };
	}

	public PropertiesConfigurationFactoryParameterizedTests(boolean ignoreUnknownFields,
			boolean usePropertySource) {
		this.factory.setIgnoreUnknownFields(ignoreUnknownFields);
		this.usePropertySource = usePropertySource;
	}

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
	public void testUnderscore() throws Exception {
		Foo foo = createFoo("spring_foo_baz: blah\nname: blah");
		assertEquals("blah", foo.spring_foo_baz);
		assertEquals("blah", foo.name);
	}

	@Test
	public void testBindToNamedTarget() throws Exception {
		this.targetName = "foo";
		Foo foo = createFoo("hi: hello\nfoo.name: foo\nfoo.bar: blah");
		assertEquals("blah", foo.bar);
	}

	@Test
	public void testBindToNamedTargetUppercaseUnderscores() throws Exception {
		this.targetName = "foo";
		Foo foo = createFoo("FOO_NAME: foo\nFOO_BAR: blah");
		assertEquals("blah", foo.bar);
	}

	private Foo createFoo(final String values) throws Exception {
		setupFactory();
		return bindFoo(values);
	}

	private Foo bindFoo(final String values) throws Exception {
		Properties properties = PropertiesLoaderUtils
				.loadProperties(new ByteArrayResource(values.getBytes()));
		if (this.usePropertySource) {
			MutablePropertySources propertySources = new MutablePropertySources();
			propertySources.addFirst(new PropertiesPropertySource("test", properties));
			this.factory.setPropertySources(propertySources);
		}
		else {
			this.factory.setProperties(properties);
		}

		this.factory.afterPropertiesSet();
		return this.factory.getObject();
	}

	private void setupFactory() throws IOException {
		this.factory.setTargetName(this.targetName);
		this.factory.setMessageSource(new StaticMessageSource());
	}

	// Foo needs to be public and to have setters for all properties
	public static class Foo {

		@NotNull
		private String name;

		private String bar;

		private String spring_foo_baz;

		private String fooBar;

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

	}

}
