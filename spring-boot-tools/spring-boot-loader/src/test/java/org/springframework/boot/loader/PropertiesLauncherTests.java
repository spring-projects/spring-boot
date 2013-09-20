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

import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class PropertiesLauncherTests {

	private PropertiesLauncher launcher = new PropertiesLauncher();

	@After
	public void close() {
		System.clearProperty("loader.home");
		System.clearProperty("loader.path");
		System.clearProperty("loader.main");
		System.clearProperty("loader.config.name");
		System.clearProperty("loader.config.location");
	}

	@Test
	public void testDefaultHome() {
		assertEquals(new File(System.getProperty("user.dir")),
				this.launcher.getHomeDirectory());
	}

	@Test
	public void testUserSpecifiedMain() throws Exception {
		this.launcher.initialize(new File("."));
		assertEquals("demo.Application", this.launcher.getMainClass(null));
	}

	@Test
	public void testUserSpecifiedConfigName() throws Exception {
		System.setProperty("loader.config.name", "foo");
		this.launcher.initialize(new File("."));
		assertEquals("my.Application", this.launcher.getMainClass(null));
		assertEquals("[etc/]", ReflectionTestUtils.getField(this.launcher, "paths")
				.toString());
	}

	@Test
	public void testSystemPropertySpecifiedMain() throws Exception {
		System.setProperty("loader.main", "foo.Bar");
		this.launcher.initialize(new File("."));
		assertEquals("foo.Bar", this.launcher.getMainClass(null));
	}

}
