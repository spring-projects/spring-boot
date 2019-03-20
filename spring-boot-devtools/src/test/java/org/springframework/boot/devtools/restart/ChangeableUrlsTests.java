/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChangeableUrls}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ChangeableUrlsTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void folderUrl() throws Exception {
		URL url = makeUrl("myproject");
		assertThat(ChangeableUrls.fromUrls(url).size()).isEqualTo(1);
	}

	@Test
	public void fileUrl() throws Exception {
		URL url = this.temporaryFolder.newFile().toURI().toURL();
		assertThat(ChangeableUrls.fromUrls(url).size()).isEqualTo(0);
	}

	@Test
	public void httpUrl() throws Exception {
		URL url = new URL("http://spring.io");
		assertThat(ChangeableUrls.fromUrls(url).size()).isEqualTo(0);
	}

	@Test
	public void skipsUrls() throws Exception {
		ChangeableUrls urls = ChangeableUrls.fromUrls(makeUrl("spring-boot"),
				makeUrl("spring-boot-autoconfigure"), makeUrl("spring-boot-actuator"),
				makeUrl("spring-boot-starter"),
				makeUrl("spring-boot-starter-some-thing"));
		assertThat(urls.size()).isEqualTo(0);
	}

	@Test
	public void urlsFromJarClassPathAreConsidered() throws Exception {
		File relative = this.temporaryFolder.newFolder();
		URL absoluteUrl = this.temporaryFolder.newFolder().toURI().toURL();
		File jarWithClassPath = makeJarFileWithUrlsInManifestClassPath(
				"project-core/target/classes/", "project-web/target/classes/",
				"does-not-exist/target/classes", relative.getName() + "/", absoluteUrl);
		new File(jarWithClassPath.getParentFile(), "project-core/target/classes")
				.mkdirs();
		new File(jarWithClassPath.getParentFile(), "project-web/target/classes").mkdirs();
		ChangeableUrls urls = ChangeableUrls
				.fromUrlClassLoader(new URLClassLoader(new URL[] {
						jarWithClassPath.toURI().toURL(), makeJarFileWithNoManifest() }));
		assertThat(urls.toList()).containsExactly(
				new URL(jarWithClassPath.toURI().toURL(), "project-core/target/classes/"),
				new URL(jarWithClassPath.toURI().toURL(), "project-web/target/classes/"),
				relative.toURI().toURL(), absoluteUrl);
	}

	private URL makeUrl(String name) throws IOException {
		File file = this.temporaryFolder.newFolder();
		file = new File(file, name);
		file = new File(file, "target");
		file = new File(file, "classes");
		file.mkdirs();
		return file.toURI().toURL();
	}

	private File makeJarFileWithUrlsInManifestClassPath(Object... urls) throws Exception {
		File classpathJar = this.temporaryFolder.newFile("classpath.jar");
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(),
				"1.0");
		manifest.getMainAttributes().putValue(Attributes.Name.CLASS_PATH.toString(),
				StringUtils.arrayToDelimitedString(urls, " "));
		new JarOutputStream(new FileOutputStream(classpathJar), manifest).close();
		return classpathJar;
	}

	private URL makeJarFileWithNoManifest() throws Exception {
		File classpathJar = this.temporaryFolder.newFile("no-manifest.jar");
		new ZipOutputStream(new FileOutputStream(classpathJar)).close();
		return classpathJar.toURI().toURL();
	}

}
