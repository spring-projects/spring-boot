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

import javax.validation.constraints.NotNull;

import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for {@link PropertiesConfigurationFactory}.
 *
 * @author Dave Syer
 */
@RunWith(Theories.class)
public class PropertiesConfigurationFactoryPerformanceTests {

	@DataPoints
	public static String[] values = new String[1000];

	private PropertiesConfigurationFactory<Foo> factory;

	private Validator validator;

	private boolean ignoreUnknownFields = true;

	private String targetName = null;

	private static StandardEnvironment environment = new StandardEnvironment();

	@BeforeClass
	public static void init() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
				"name=blah", "bar=blah");
	}

	@Theory
	public void testValidProperties(String value) throws Exception {
		Foo foo = createFoo();
		assertThat(foo.bar).isEqualTo("blah");
		assertThat(foo.name).isEqualTo("blah");
	}

	private Foo createFoo() throws Exception {
		setupFactory();
		this.factory.setPropertySources(environment.getPropertySources());
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
