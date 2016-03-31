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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesLauncher}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class PropertiesLauncherTests {

	@Rule
	public InternalOutputCapture output = new InternalOutputCapture();

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);
		System.setProperty("loader.home",
				new File("src/test/resources").getAbsolutePath());
	}

	@After
	public void close() {
		System.clearProperty("loader.home");
		System.clearProperty("loader.path");
		System.clearProperty("loader.main");
		System.clearProperty("loader.config.name");
		System.clearProperty("loader.config.location");
		System.clearProperty("loader.system");
	}

	@Test
	public void testDefaultHome() {
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getHomeDirectory())
				.isEqualTo(new File(System.getProperty("loader.home")));
	}

	@Test
	public void testUserSpecifiedMain() throws Exception {
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("demo.Application");
		assertThat(System.getProperty("loader.main")).isNull();
	}

	@Test
	public void testUserSpecifiedConfigName() throws Exception {
		System.setProperty("loader.config.name", "foo");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("my.Application");
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[etc/]");
	}

	@Test
	public void testUserSpecifiedDotPath() throws Exception {
		System.setProperty("loader.path", ".");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[.]");
	}

	@Test
	public void testUserSpecifiedWildcardPath() throws Exception {
		System.setProperty("loader.path", "jars/*");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedJarPath() throws Exception {
		System.setProperty("loader.path", "jars/app.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedJarFileWithNestedArchives() throws Exception {
		System.setProperty("loader.path", "nested-jars/app.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedDirectoryContainingJarFileWithNestedArchives()
			throws Exception {
		System.setProperty("loader.path", "nested-jars");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedJarPathWithDot() throws Exception {
		System.setProperty("loader.path", "./jars/app.jar");
		System.setProperty("loader.main", "demo.Application");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedClassLoader() throws Exception {
		System.setProperty("loader.path", "jars/app.jar");
		System.setProperty("loader.classLoader", URLClassLoader.class.getName());
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello World");
	}

	@Test
	public void testUserSpecifiedClassPathOrder() throws Exception {
		System.setProperty("loader.path", "more-jars/app.jar,jars/app.jar");
		System.setProperty("loader.classLoader", URLClassLoader.class.getName());
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
				.isEqualTo("[more-jars/app.jar, jars/app.jar]");
		launcher.launch(new String[0]);
		waitFor("Hello Other World");
	}

	@Test
	public void testCustomClassLoaderCreation() throws Exception {
		System.setProperty("loader.classLoader", TestLoader.class.getName());
		PropertiesLauncher launcher = new PropertiesLauncher();
		ClassLoader loader = launcher.createClassLoader(Collections.<Archive>emptyList());
		assertThat(loader).isNotNull();
		assertThat(loader.getClass().getName()).isEqualTo(TestLoader.class.getName());
	}

	@Test
	public void testUserSpecifiedConfigPathWins() throws Exception {

		System.setProperty("loader.config.name", "foo");
		System.setProperty("loader.config.location", "classpath:bar.properties");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("my.BarApplication");
	}

	@Test
	public void testSystemPropertySpecifiedMain() throws Exception {
		System.setProperty("loader.main", "foo.Bar");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(launcher.getMainClass()).isEqualTo("foo.Bar");
	}

	@Test
	public void testSystemPropertiesSet() throws Exception {
		System.setProperty("loader.system", "true");
		new PropertiesLauncher();
		assertThat(System.getProperty("loader.main")).isEqualTo("demo.Application");
	}

	@Test
	public void testArgsEnhanced() throws Exception {
		System.setProperty("loader.args", "foo");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertThat(Arrays.asList(launcher.getArgs("bar")).toString())
				.isEqualTo("[foo, bar]");
	}

	private void waitFor(String value) throws Exception {
		int count = 0;
		boolean timeout = false;
		while (!timeout && count < 100) {
			count++;
			Thread.sleep(50L);
			timeout = this.output.toString().contains(value);
		}
		assertThat(timeout).as("Timed out waiting for (" + value + ")").isTrue();
	}

	public static class TestLoader extends URLClassLoader {

		public TestLoader(ClassLoader parent) {
			super(new URL[0], parent);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			return super.findClass(name);
		}
	}

}
