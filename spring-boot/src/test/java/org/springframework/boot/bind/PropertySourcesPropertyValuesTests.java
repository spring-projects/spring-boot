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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.validation.DataBinder;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link PropertySourcesPropertyValues}.
 *
 * @author Dave Syer
 */
public class PropertySourcesPropertyValuesTests {

	private final MutablePropertySources propertySources = new MutablePropertySources();

	@Before
	public void init() {
		this.propertySources.addFirst(new PropertySource<String>("static", "foo") {
			@Override
			public Object getProperty(String name) {
				if (name.equals(getSource())) {
					return "bar";
				}
				return null;
			}

		});
		this.propertySources.addFirst(new MapPropertySource("map", Collections
				.<String, Object> singletonMap("name", "${foo}")));
	}

	@Test
	public void testSize() {
		PropertySourcesPropertyValues propertyValues = new PropertySourcesPropertyValues(
				this.propertySources);
		assertEquals(1, propertyValues.getPropertyValues().length);
	}

	@Test
	public void testNonEnumeratedValue() {
		PropertySourcesPropertyValues propertyValues = new PropertySourcesPropertyValues(
				this.propertySources);
		assertEquals("bar", propertyValues.getPropertyValue("foo").getValue());
	}

	@Test
	public void testEnumeratedValue() {
		PropertySourcesPropertyValues propertyValues = new PropertySourcesPropertyValues(
				this.propertySources);
		assertEquals("bar", propertyValues.getPropertyValue("name").getValue());
	}

	@Test
	public void testOverriddenValue() {
		this.propertySources.addFirst(new MapPropertySource("new", Collections
				.<String, Object> singletonMap("name", "spam")));
		PropertySourcesPropertyValues propertyValues = new PropertySourcesPropertyValues(
				this.propertySources);
		assertEquals("spam", propertyValues.getPropertyValue("name").getValue());
	}

	@Test
	public void testPlaceholdersBinding() {
		TestBean target = new TestBean();
		DataBinder binder = new DataBinder(target);
		binder.bind(new PropertySourcesPropertyValues(this.propertySources));
		assertEquals("bar", target.getName());
	}

	@Test
	public void testPlaceholdersBindingNonEnumerable() {
		FooBean target = new FooBean();
		DataBinder binder = new DataBinder(target);
		binder.bind(new PropertySourcesPropertyValues(this.propertySources, null,
				Collections.singleton("foo")));
		assertEquals("bar", target.getFoo());
	}

	@Test
	public void testPlaceholdersBindingWithError() {
		TestBean target = new TestBean();
		DataBinder binder = new DataBinder(target);
		this.propertySources.addFirst(new MapPropertySource("another", Collections
				.<String, Object> singletonMap("something", "${nonexistent}")));
		binder.bind(new PropertySourcesPropertyValues(this.propertySources));
		assertEquals("bar", target.getName());
	}

	public static class TestBean {
		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class FooBean {
		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

}
