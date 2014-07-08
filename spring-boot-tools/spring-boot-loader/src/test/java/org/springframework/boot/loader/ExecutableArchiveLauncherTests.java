/*
 * Copyright 2012-2014 the original author or authors.
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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.loader.archive.Archive.Entry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ExecutableArchiveLauncher}
 *
 * @author Andy Wilkinson
 */
public class ExecutableArchiveLauncherTests {

	@Mock
	private JavaAgentDetector javaAgentDetector;

	private ExecutableArchiveLauncher launcher;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);

		this.launcher = new UnitTestExecutableArchiveLauncher(this.javaAgentDetector);
	}

	@Test
	public void createdClassLoaderContainsUrlsFromThreadContextClassLoader()
			throws Exception {
		final URL[] urls = new URL[] { new URL("file:one"), new URL("file:two") };

		doWithTccl(new URLClassLoader(urls), new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				ClassLoader classLoader = ExecutableArchiveLauncherTests.this.launcher
						.createClassLoader(new URL[0]);
				assertClassLoaderUrls(classLoader, urls);
				return null;
			}
		});
	}

	@Test
	public void javaAgentJarsAreExcludedFromClasspath() throws Exception {
		URL javaAgent = new File("my-agent.jar").getCanonicalFile().toURI().toURL();
		final URL one = new URL("file:one");

		when(this.javaAgentDetector.isJavaAgentJar(javaAgent)).thenReturn(true);

		doWithTccl(new URLClassLoader(new URL[] { javaAgent, one }),
				new Callable<Void>() {

					@Override
					public Void call() throws Exception {
						ClassLoader classLoader = ExecutableArchiveLauncherTests.this.launcher
								.createClassLoader(new URL[0]);
						assertClassLoaderUrls(classLoader, new URL[] { one });
						return null;
					}
				});
	}

	private void assertClassLoaderUrls(ClassLoader classLoader, URL[] urls) {
		assertTrue(classLoader instanceof URLClassLoader);
		assertArrayEquals(urls, ((URLClassLoader) classLoader).getURLs());
	}

	private static final class UnitTestExecutableArchiveLauncher extends
			ExecutableArchiveLauncher {

		public UnitTestExecutableArchiveLauncher(JavaAgentDetector javaAgentDetector) {
			super(javaAgentDetector);
		}

		@Override
		protected boolean isNestedArchive(Entry entry) {
			return false;
		}
	}

	private void doWithTccl(ClassLoader classLoader, Callable<?> action) throws Exception {
		ClassLoader old = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			action.call();
		}
		finally {
			Thread.currentThread().setContextClassLoader(old);
		}
	}

}
