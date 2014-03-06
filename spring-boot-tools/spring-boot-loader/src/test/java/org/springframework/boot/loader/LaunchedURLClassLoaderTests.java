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

import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class LaunchedURLClassLoaderTests {

	@Test
	public void resolveResourceFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { new URL(
				"jar:file:src/test/resources/jars/app.jar!/") }, getClass()
				.getClassLoader());
		assertNotNull(loader.getResource("demo/Application.java"));
	}

	@Test
	public void resolveResourcesFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { new URL(
				"jar:file:src/test/resources/jars/app.jar!/") }, getClass()
				.getClassLoader());
		assertTrue(loader.getResources("demo/Application.java").hasMoreElements());
	}

	@Test
	public void resolveRootPathFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { new URL(
				"jar:file:src/test/resources/jars/app.jar!/") }, getClass()
				.getClassLoader());
		assertNotNull(loader.getResource(""));
	}

	@Test
	public void resolveRootResourcesFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { new URL(
				"jar:file:src/test/resources/jars/app.jar!/") }, getClass()
				.getClassLoader());
		assertTrue(loader.getResources("").hasMoreElements());
	}

}
