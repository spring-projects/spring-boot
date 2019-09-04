/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StaticResourceJars}.
 *
 * @author Rupert Madden-Abbott
 * @author Andy Wilkinson
 */
class StaticResourceJarsTests {

	@TempDir
	File tempDir;

	@Test
	void includeJarWithStaticResources() throws Exception {
		File jarFile = createResourcesJar("test-resources.jar");
		List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
		assertThat(staticResourceJarUrls).hasSize(1);
	}

	@Test
	void includeJarWithStaticResourcesWithUrlEncodedSpaces() throws Exception {
		File jarFile = createResourcesJar("test resources.jar");
		List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
		assertThat(staticResourceJarUrls).hasSize(1);
	}

	@Test
	void includeJarWithStaticResourcesWithPlusInItsPath() throws Exception {
		File jarFile = createResourcesJar("test + resources.jar");
		List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
		assertThat(staticResourceJarUrls).hasSize(1);
	}

	@Test
	void excludeJarWithoutStaticResources() throws Exception {
		File jarFile = createJar("dependency.jar");
		List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL());
		assertThat(staticResourceJarUrls).hasSize(0);
	}

	@Test
	void uncPathsAreTolerated() throws Exception {
		File jarFile = createResourcesJar("test-resources.jar");
		List<URL> staticResourceJarUrls = new StaticResourceJars().getUrlsFrom(jarFile.toURI().toURL(),
				new URL("file://unc.example.com/test.jar"));
		assertThat(staticResourceJarUrls).hasSize(1);
	}

	private File createResourcesJar(String name) throws IOException {
		return createJar(name, (output) -> {
			JarEntry jarEntry = new JarEntry("META-INF/resources");
			try {
				output.putNextEntry(jarEntry);
				output.closeEntry();
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	private File createJar(String name) throws IOException {
		return createJar(name, null);
	}

	private File createJar(String name, Consumer<JarOutputStream> customizer) throws IOException {
		File jarFile = new File(this.tempDir, name);
		JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
		if (customizer != null) {
			customizer.accept(jarOutputStream);
		}
		jarOutputStream.close();
		return jarFile;
	}

}
