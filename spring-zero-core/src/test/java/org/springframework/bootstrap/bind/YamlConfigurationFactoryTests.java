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

package org.springframework.bootstrap.bind;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.constraints.NotNull;

import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.BindException;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.yaml.snakeyaml.error.YAMLException;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class YamlConfigurationFactoryTests {

	private YamlConfigurationFactory<Foo> factory;

	private Validator validator;

	private Map<Class<?>, Map<String, String>> aliases = new HashMap<Class<?>, Map<String, String>>();

	private Foo createFoo(final String yaml) throws Exception {
		this.factory = new YamlConfigurationFactory<Foo>(Foo.class);
		this.factory.setYaml(yaml);
		this.factory.setExceptionIfInvalid(true);
		this.factory.setPropertyAliases(this.aliases);
		this.factory.setValidator(this.validator);
		this.factory.setMessageSource(new StaticMessageSource());
		this.factory.afterPropertiesSet();
		return this.factory.getObject();
	}

	@Test
	public void testValidYamlLoadsWithNoErrors() throws Exception {
		Foo foo = createFoo("name: blah\nbar: blah");
		assertEquals("blah", foo.bar);
	}

	@Test
	public void testValidYamlWithAliases() throws Exception {
		this.aliases.put(Foo.class, Collections.singletonMap("foo-name", "name"));
		Foo foo = createFoo("foo-name: blah\nbar: blah");
		assertEquals("blah", foo.name);
	}

	@Test(expected = YAMLException.class)
	public void unknownPropertyCausesLoadFailure() throws Exception {
		createFoo("hi: hello\nname: foo\nbar: blah");
	}

	@Test(expected = BindException.class)
	public void missingPropertyCausesValidationError() throws Exception {
		this.validator = new SpringValidatorAdapter(Validation
				.buildDefaultValidatorFactory().getValidator());
		createFoo("bar: blah");
	}

	private static class Foo {
		@NotNull
		public String name;

		public String bar;
	}

}
