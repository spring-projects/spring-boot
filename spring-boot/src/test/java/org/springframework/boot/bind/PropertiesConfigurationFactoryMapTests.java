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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesConfigurationFactory} binding to a map.
 *
 * @author Dave Syer
 */
public class PropertiesConfigurationFactoryMapTests {

	private PropertiesConfigurationFactory<Foo> factory;

	private Validator validator;

	private boolean ignoreUnknownFields = true;

	private String targetName = null;

	@Test
	public void testValidPropertiesLoadsWithNoErrors() throws Exception {
		Foo foo = createFoo("map.name: blah\nmap.bar: blah");
		assertThat(foo.map.get("bar")).isEqualTo("blah");
		assertThat(foo.map.get("name")).isEqualTo("blah");
	}

	@Test
	public void testBindToNamedTarget() throws Exception {
		this.targetName = "foo";
		Foo foo = createFoo("hi: hello\nfoo.map.name: foo\nfoo.map.bar: blah");
		assertThat(foo.map.get("bar")).isEqualTo("blah");
	}

	@Test
	public void testBindFromPropertySource() throws Exception {
		this.targetName = "foo";
		setupFactory();
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("map",
				Collections.singletonMap("foo.map.name", (Object) "blah")));
		this.factory.setPropertySources(sources);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.map.get("name")).isEqualTo("blah");
	}

	@Test
	public void testBindFromCompositePropertySource() throws Exception {
		this.targetName = "foo";
		setupFactory();
		MutablePropertySources sources = new MutablePropertySources();
		CompositePropertySource composite = new CompositePropertySource("composite");
		composite.addPropertySource(new MapPropertySource("map",
				Collections.singletonMap("foo.map.name", (Object) "blah")));
		sources.addFirst(composite);
		this.factory.setPropertySources(sources);
		this.factory.afterPropertiesSet();
		Foo foo = this.factory.getObject();
		assertThat(foo.map.get("name")).isEqualTo("blah");
	}

	private Foo createFoo(final String values) throws Exception {
		setupFactory();
		return bindFoo(values);
	}

	@Deprecated
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
		private Map<String, Object> map = new HashMap<String, Object>();

		public Map<String, Object> getMap() {
			return this.map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}
	}

}
