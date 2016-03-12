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

package org.springframework.boot.cli.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplicationLauncher}
 *
 * @author Andy Wilkinson
 */
public class SpringApplicationLauncherTests {

	private Map<String, String> env = new HashMap<String, String>();

	@After
	public void cleanUp() {
		System.clearProperty("spring.application.class.name");
	}

	@Test
	public void defaultLaunch() throws Exception {
		assertThat(launch()).contains("org.springframework.boot.SpringApplication");
	}

	@Test
	public void launchWithClassConfiguredBySystemProperty() {
		System.setProperty("spring.application.class.name",
				"system.property.SpringApplication");
		assertThat(launch()).contains("system.property.SpringApplication");
	}

	@Test
	public void launchWithClassConfiguredByEnvironmentVariable() {
		this.env.put("SPRING_APPLICATION_CLASS_NAME",
				"environment.variable.SpringApplication");
		assertThat(launch()).contains("environment.variable.SpringApplication");
	}

	@Test
	public void systemPropertyOverridesEnvironmentVariable() {
		System.setProperty("spring.application.class.name",
				"system.property.SpringApplication");
		this.env.put("SPRING_APPLICATION_CLASS_NAME",
				"environment.variable.SpringApplication");
		assertThat(launch()).contains("system.property.SpringApplication");

	}

	@Test
	public void sourcesDefaultPropertiesAndArgsAreUsedToLaunch() throws Exception {
		System.setProperty("spring.application.class.name",
				TestSpringApplication.class.getName());
		Object[] sources = new Object[0];
		String[] args = new String[0];
		new SpringApplicationLauncher(getClass().getClassLoader()).launch(sources, args);

		assertThat(sources == TestSpringApplication.sources).isTrue();
		assertThat(args == TestSpringApplication.args).isTrue();

		Map<String, String> defaultProperties = TestSpringApplication.defaultProperties;
		assertThat(defaultProperties).hasSize(1)
				.containsEntry("spring.groovy.template.check-template-location", "false");
	}

	private Set<String> launch() {
		TestClassLoader classLoader = new TestClassLoader(getClass().getClassLoader());
		try {
			new TestSpringApplicationLauncher(classLoader).launch(new Object[0],
					new String[0]);
		}
		catch (Exception ex) {
			// Launch will fail, but we can still check that the launcher tried to use
			// the right class
		}
		return classLoader.classes;
	}

	private static class TestClassLoader extends ClassLoader {

		private Set<String> classes = new HashSet<String>();

		TestClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			this.classes.add(name);
			return super.loadClass(name, resolve);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			this.classes.add(name);
			return super.findClass(name);
		}

	}

	public static class TestSpringApplication {

		private static Object[] sources;

		private static Map<String, String> defaultProperties;

		private static String[] args;

		public TestSpringApplication(Object[] sources) {
			TestSpringApplication.sources = sources;
		}

		public void setDefaultProperties(Map<String, String> defaultProperties) {
			TestSpringApplication.defaultProperties = defaultProperties;
		}

		public void run(String[] args) {
			TestSpringApplication.args = args;
		}
	}

	private class TestSpringApplicationLauncher extends SpringApplicationLauncher {

		TestSpringApplicationLauncher(ClassLoader classLoader) {
			super(classLoader);
		}

		@Override
		protected String getEnvironmentVariable(String name) {
			String variable = SpringApplicationLauncherTests.this.env.get(name);
			if (variable == null) {
				variable = super.getEnvironmentVariable(name);
			}
			return variable;
		}

	}

}
