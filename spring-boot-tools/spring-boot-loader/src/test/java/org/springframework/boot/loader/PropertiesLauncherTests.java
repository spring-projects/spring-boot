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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link PropertiesLauncher}.
 * 
 * @author Dave Syer
 */
public class PropertiesLauncherTests {

	@Before
	public void setup() throws IOException {
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
		assertEquals(new File(System.getProperty("loader.home")),
				launcher.getHomeDirectory());
	}

	@Test
	public void testUserSpecifiedMain() throws Exception {
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertEquals("demo.Application", launcher.getMainClass());
		assertEquals(null, System.getProperty("loader.main"));
	}

	@Test
	public void testUserSpecifiedConfigName() throws Exception {

		System.setProperty("loader.config.name", "foo");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertEquals("my.Application", launcher.getMainClass());
		assertEquals("[etc/]", ReflectionTestUtils.getField(launcher, "paths").toString());
	}

	@Test
	public void testUserSpecifiedConfigPathWins() throws Exception {

		System.setProperty("loader.config.name", "foo");
		System.setProperty("loader.config.location", "classpath:bar.properties");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertEquals("my.BarApplication", launcher.getMainClass());
	}

	@Test
	public void testSystemPropertySpecifiedMain() throws Exception {
		System.setProperty("loader.main", "foo.Bar");
		PropertiesLauncher launcher = new PropertiesLauncher();
		assertEquals("foo.Bar", launcher.getMainClass());
	}

	@Test
	public void testSystemPropertiesSet() throws Exception {
		System.setProperty("loader.system", "true");
		new PropertiesLauncher();
		assertEquals("demo.Application", System.getProperty("loader.main"));
	}

}
