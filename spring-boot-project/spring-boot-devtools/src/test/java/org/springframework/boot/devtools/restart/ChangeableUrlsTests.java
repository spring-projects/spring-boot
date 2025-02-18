/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChangeableUrls}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ChangeableUrlsTests {

	@TempDir
	File tempDir;

	@Test
	void directoryUrl() throws Exception {
		URL url = makeUrl("myproject");
		assertThat(ChangeableUrls.fromUrls(url)).hasSize(1);
	}

	@Test
	void fileUrl() throws Exception {
		File file = new File(this.tempDir, "file");
		file.createNewFile();
		URL url = file.toURI().toURL();
		assertThat(ChangeableUrls.fromUrls(url)).isEmpty();
	}

	@Test
	void httpUrl() throws Exception {
		URL url = new URL("https://spring.io");
		assertThat(ChangeableUrls.fromUrls(url)).isEmpty();
	}

	@Test
	void httpsUrl() throws Exception {
		URL url = new URL("https://spring.io");
		assertThat(ChangeableUrls.fromUrls(url)).isEmpty();
	}

	@Test
	void skipsUrls() throws Exception {
		ChangeableUrls urls = ChangeableUrls.fromUrls(makeUrl("spring-boot"), makeUrl("spring-boot-autoconfigure"),
				makeUrl("spring-boot-actuator"), makeUrl("spring-boot-starter"),
				makeUrl("spring-boot-starter-some-thing"));
		assertThat(urls).isEmpty();
	}

	@Test
	void urlsFromJarClassPathAreConsidered() throws Exception {
		File relative = new File(this.tempDir, UUID.randomUUID().toString());
		relative.mkdir();
		File absolute = new File(this.tempDir, UUID.randomUUID().toString());
		absolute.mkdirs();
		URL absoluteUrl = absolute.toURI().toURL();
		File jarWithClassPath = makeJarFileWithUrlsInManifestClassPath("project-core/target/classes/",
				"project-web/target/classes/", "project%20space/target/classes/", "does-not-exist/target/classes/",
				relative.getName() + "/", absoluteUrl);
		new File(jarWithClassPath.getParentFile(), "project-core/target/classes").mkdirs();
		new File(jarWithClassPath.getParentFile(), "project-web/target/classes").mkdirs();
		new File(jarWithClassPath.getParentFile(), "project space/target/classes").mkdirs();
		ChangeableUrls urls = ChangeableUrls.fromClassLoader(
				new URLClassLoader(new URL[] { jarWithClassPath.toURI().toURL(), makeJarFileWithNoManifest() }));
		assertThat(urls.toList()).containsExactly(
				new URL(jarWithClassPath.toURI().toURL(), "project-core/target/classes/"),
				new URL(jarWithClassPath.toURI().toURL(), "project-web/target/classes/"),
				new URL(jarWithClassPath.toURI().toURL(), "project space/target/classes/"), relative.toURI().toURL(),
				absoluteUrl);
	}

	private URL makeUrl(String name) throws IOException {
		File file = new File(this.tempDir, UUID.randomUUID().toString());
		file = new File(file, name);
		file = new File(file, "build");
		file = new File(file, "classes");
		file.mkdirs();
		return file.toURI().toURL();
	}

	private File makeJarFileWithUrlsInManifestClassPath(Object... urls) throws Exception {
		File classpathJar = new File(this.tempDir, "classpath.jar");
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
		manifest.getMainAttributes()
			.putValue(Attributes.Name.CLASS_PATH.toString(), StringUtils.arrayToDelimitedString(urls, " "));
		new JarOutputStream(new FileOutputStream(classpathJar), manifest).close();
		return classpathJar;
	}

	private URL makeJarFileWithNoManifest() throws Exception {
		File classpathJar = new File(this.tempDir, "no-manifest.jar");
		new ZipOutputStream(new FileOutputStream(classpathJar)).close();
		return classpathJar.toURI().toURL();
	}

}
