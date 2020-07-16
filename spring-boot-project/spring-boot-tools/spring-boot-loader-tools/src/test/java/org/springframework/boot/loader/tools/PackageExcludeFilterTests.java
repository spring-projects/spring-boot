/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PackageExcludeFilter}
 *
 * @author Christoph Dreis
 */
class PackageExcludeFilterTests {

	@TempDir
	File temp;

	@Test
	void defaultFilterExcludesSpringBootStarter() throws IOException {
		String name = "spring-boot-starter-web-0.1.2.jar";
		File file = springBootJarFile(name, true);
		assertThat(PackageExcludeFilter.DEFAULT.isExcluded(name, file)).isTrue();
	}

	@Test
	void defaultFilterExcludesSpringBootConfigurationProcessor() throws IOException {
		String name = "spring-boot-configuration-processor-0.1.2.jar";
		File file = springBootJarFile(name, true);
		assertThat(PackageExcludeFilter.DEFAULT.isExcluded(name, file)).isTrue();
	}

	@Test
	void defaultFilterDoesNotExcludeWithoutManifest() throws IOException {
		String name = "spring-boot-starter-web-0.1.2.jar";
		File file = springBootJarFile(name, false);
		assertThat(PackageExcludeFilter.DEFAULT.isExcluded(name, file)).isFalse();
	}

	@Test
	void defaultFilterDoesNotExcludeWithNonMatchingName() throws IOException {
		String name = "test.jar";
		File file = springBootJarFile(name, true);
		assertThat(PackageExcludeFilter.DEFAULT.isExcluded(name, file)).isFalse();
	}

	private File newFile(String name) throws IOException {
		File file = new File(this.temp, name);
		file.createNewFile();
		return file;
	}

	private File springBootJarFile(String name, boolean withManifest) throws IOException {
		File file = newFile(name);
		if (withManifest) {
			try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
				jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
				Manifest manifest = new Manifest();
				Attributes attributes = manifest.getMainAttributes();
				attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
				attributes.putValue("Exclude-From-Packaging", name);
				manifest.write(jar);
				jar.closeEntry();
			}
		}
		return file;
	}

}
