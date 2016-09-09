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

package org.springframework.boot.env;

import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link SpringApplicationJsonEnvironmentPostProcessor}.
 *
 * @author Dave Syer
 */
public class SpringApplicationJsonEnvironmentPostProcessorTests {

	private SpringApplicationJsonEnvironmentPostProcessor processor = new SpringApplicationJsonEnvironmentPostProcessor();

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@Test
	public void error() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.application.json=foo:bar");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
	}

	@Test
	public void missing() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
	}

	@Test
	public void empty() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.application.json={}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
	}

	@Test
	public void periodSeparated() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.application.json={\"foo\":\"bar\"}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("bar", this.environment.resolvePlaceholders("${foo:}"));
	}

	@Test
	public void envVar() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"SPRING_APPLICATION_JSON={\"foo\":\"bar\"}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("bar", this.environment.resolvePlaceholders("${foo:}"));
	}

	@Test
	public void nested() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"SPRING_APPLICATION_JSON={\"foo\":{\"bar\":\"spam\",\"rab\":\"maps\"}}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("spam", this.environment.resolvePlaceholders("${foo.bar:}"));
		assertEquals("maps", this.environment.resolvePlaceholders("${foo.rab:}"));
	}

	@Test
	public void prefixed() {
		assertEquals("", this.environment.resolvePlaceholders("${foo:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"SPRING_APPLICATION_JSON={\"foo.bar\":\"spam\"}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("spam", this.environment.resolvePlaceholders("${foo.bar:}"));
	}

	@Test
	public void list() {
		assertEquals("", this.environment.resolvePlaceholders("${foo[1]:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"SPRING_APPLICATION_JSON={\"foo\":[\"bar\",\"spam\"]}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("spam", this.environment.resolvePlaceholders("${foo[1]:}"));
	}

	@Test
	public void listOfObject() {
		assertEquals("", this.environment.resolvePlaceholders("${foo[0].bar:}"));
		EnvironmentTestUtils.addEnvironment(this.environment,
				"SPRING_APPLICATION_JSON={\"foo\":[{\"bar\":\"spam\"}]}");
		this.processor.postProcessEnvironment(this.environment, null);
		assertEquals("spam", this.environment.resolvePlaceholders("${foo[0].bar:}"));
	}

}
