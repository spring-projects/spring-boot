/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.launch;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.net.protocol.jar.JarUrl;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WarLauncher}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class WarLauncherTests extends AbstractLauncherTests {

	@Test
	void explodedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
		File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF"));
		WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot));
		Set<URL> urls = launcher.getClassPathUrls();
		assertThat(urls).containsExactlyInAnyOrder(getExpectedFileUrls(explodedRoot));
	}

	@Test
	void archivedWarHasOnlyWebInfClassesAndContentsOfWebInfLibOnClasspath() throws Exception {
		File file = createJarArchive("archive.war", "WEB-INF");
		try (JarFileArchive archive = new JarFileArchive(file)) {
			WarLauncher launcher = new WarLauncher(archive);
			Set<URL> urls = launcher.getClassPathUrls();
			List<URL> expected = new ArrayList<>();
			expected.add(JarUrl.create(file, "WEB-INF/classes/"));
			expected.add(JarUrl.create(file, "WEB-INF/lib/foo.jar"));
			expected.add(JarUrl.create(file, "WEB-INF/lib/bar.jar"));
			expected.add(JarUrl.create(file, "WEB-INF/lib/baz.jar"));
			assertThat(urls).containsOnly(expected.toArray(URL[]::new));
		}
	}

	@Test
	void explodedWarShouldPreserveClasspathOrderWhenIndexPresent() throws Exception {
		File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF", true, Collections.emptyList()));
		WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot));
		URLClassLoader classLoader = createClassLoader(launcher);
		URL[] urls = classLoader.getURLs();
		assertThat(urls).containsExactly(getExpectedFileUrls(explodedRoot));
	}

	@Test
	void warFilesPresentInWebInfLibsAndNotInClasspathIndexShouldBeAddedAfterWebInfClasses() throws Exception {
		ArrayList<String> extraLibs = new ArrayList<>(Arrays.asList("extra-1.jar", "extra-2.jar"));
		File explodedRoot = explode(createJarArchive("archive.war", "WEB-INF", true, extraLibs));
		WarLauncher launcher = new WarLauncher(new ExplodedArchive(explodedRoot));
		URLClassLoader classLoader = createClassLoader(launcher);
		URL[] urls = classLoader.getURLs();
		List<File> expectedFiles = getExpectedFilesWithExtraLibs(explodedRoot);
		URL[] expectedFileUrls = expectedFiles.stream().map(this::toUrl).toArray(URL[]::new);
		assertThat(urls).containsExactly(expectedFileUrls);
	}

	private URL[] getExpectedFileUrls(File explodedRoot) {
		return getExpectedFiles(explodedRoot).stream().map(this::toUrl).toArray(URL[]::new);
	}

	private List<File> getExpectedFiles(File parent) {
		List<File> expected = new ArrayList<>();
		expected.add(new File(parent, "WEB-INF/classes"));
		expected.add(new File(parent, "WEB-INF/lib/foo.jar"));
		expected.add(new File(parent, "WEB-INF/lib/bar.jar"));
		expected.add(new File(parent, "WEB-INF/lib/baz.jar"));
		return expected;
	}

	private List<File> getExpectedFilesWithExtraLibs(File parent) {
		List<File> expected = new ArrayList<>();
		expected.add(new File(parent, "WEB-INF/classes"));
		expected.add(new File(parent, "WEB-INF/lib/extra-1.jar"));
		expected.add(new File(parent, "WEB-INF/lib/extra-2.jar"));
		expected.add(new File(parent, "WEB-INF/lib/foo.jar"));
		expected.add(new File(parent, "WEB-INF/lib/bar.jar"));
		expected.add(new File(parent, "WEB-INF/lib/baz.jar"));
		return expected;
	}

}
