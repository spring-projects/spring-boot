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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class BootJarTests extends AbstractBootArchiveTests<BootJar> {

	BootJarTests() {
		super(BootJar.class, "org.springframework.boot.loader.JarLauncher", "BOOT-INF/lib/", "BOOT-INF/classes/");
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecFromGetter() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.getBootInf().into("test").from(new File("build.gradle").getAbsolutePath());
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecAction() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.bootInf((copySpec) -> copySpec.into("test").from(new File("build.gradle").getAbsolutePath()));
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void layers() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Main");
		bootJar.layered();
		File classesJavaMain = new File(this.temp, "classes/java/main");
		File applicationClass = new File(classesJavaMain, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		File resourcesMain = new File(this.temp, "resources/main");
		File applicationProperties = new File(resourcesMain, "application.properties");
		applicationProperties.getParentFile().mkdirs();
		applicationProperties.createNewFile();
		File staticResources = new File(resourcesMain, "static");
		staticResources.mkdir();
		File css = new File(staticResources, "test.css");
		css.createNewFile();
		bootJar.classpath(classesJavaMain, resourcesMain, jarFile("first-library.jar"), jarFile("second-library.jar"),
				jarFile("third-library-SNAPSHOT.jar"));
		bootJar.requiresUnpack("second-library.jar");
		executeTask();
		List<String> entryNames = getEntryNames(bootJar.getArchiveFile().get().getAsFile());
		assertThat(entryNames).containsSubsequence("org/springframework/boot/loader/",
				"BOOT-INF/layers/application/classes/com/example/Application.class",
				"BOOT-INF/layers/resources/classes/static/test.css",
				"BOOT-INF/layers/application/classes/application.properties",
				"BOOT-INF/layers/dependencies/lib/first-library.jar",
				"BOOT-INF/layers/dependencies/lib/second-library.jar",
				"BOOT-INF/layers/snapshot-dependencies/lib/third-library-SNAPSHOT.jar");
		assertThat(entryNames).doesNotContain("BOOT-INF/classes").doesNotContain("BOOT-INF/lib")
				.doesNotContain("BOOT-INF/com/");
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes")).isEqualTo(null);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isEqualTo(null);
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Layers-Index"))
					.isEqualTo("BOOT-INF/layers.idx");
			try (InputStream input = jarFile.getInputStream(jarFile.getEntry("BOOT-INF/layers.idx"))) {
				assertThat(input).hasContent("dependencies\nsnapshot-dependencies\nresources\napplication\n");
			}
		}
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

}
