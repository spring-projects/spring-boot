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

package org.springframework.bootstrap.context.initializer;

import org.junit.Test;
import org.springframework.bootstrap.TestUtils;
import org.springframework.bootstrap.context.initializer.ContextIdApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ContextIdApplicationContextInitializer}.
 * 
 * @author Dave Syer
 */
public class ContextIdApplicationContextInitializerTests {

	private ContextIdApplicationContextInitializer initializer = new ContextIdApplicationContextInitializer();

	@Test
	public void testDefaults() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		assertEquals("application", context.getId());
	}

	@Test
	public void testNameAndPort() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(context, "spring.application.name:foo", "PORT:8080");
		this.initializer.initialize(context);
		assertEquals("foo:8080", context.getId());
	}

	@Test
	public void testNameAndProfiles() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(context, "spring.application.name:foo",
				"spring.profiles.active: spam,bar");
		this.initializer.initialize(context);
		assertEquals("foo:spam,bar", context.getId());
	}

	@Test
	public void testCloudFoundry() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(context, "spring.config.name:foo", "PORT:8080",
				"vcap.application.name:bar", "vcap.application.instance_index:2");
		this.initializer.initialize(context);
		assertEquals("bar:2", context.getId());
	}

	@Test
	public void testExplicitName() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(context, "spring.application.name:spam",
				"spring.config.name:foo", "PORT:8080",
				"vcap.application.application_name:bar",
				"vcap.application.instance_index:2");
		this.initializer.initialize(context);
		assertEquals("spam:2", context.getId());
	}

}
