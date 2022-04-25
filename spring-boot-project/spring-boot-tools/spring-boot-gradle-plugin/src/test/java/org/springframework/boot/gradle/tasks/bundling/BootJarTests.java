/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Paddy Drury
 */
@ClassPathExclusions("kotlin-daemon-client-*")
class BootJarTests extends AbstractBootArchiveTests<BootJar> {

	BootJarTests() {
		super(BootJar.class, "org.springframework.boot.loader.JarLauncher", "BOOT-INF/lib/", "BOOT-INF/classes/",
				"BOOT-INF/");
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecFromGetter() throws IOException {
		BootJar bootJar = getTask();
		bootJar.getMainClass().set("com.example.Application");
		bootJar.getBootInf().into("test").from(new File("build.gradle").getAbsolutePath());
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecAction() throws IOException {
		BootJar bootJar = getTask();
		bootJar.getMainClass().set("com.example.Application");
		bootJar.bootInf((copySpec) -> copySpec.into("test").from(new File("build.gradle").getAbsolutePath()));
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void jarsInLibAreStored() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getEntry("BOOT-INF/lib/first-library.jar").getMethod()).isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/second-library.jar").getMethod()).isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/third-library-SNAPSHOT.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/first-project-library.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
			assertThat(jarFile.getEntry("BOOT-INF/lib/second-project-library-SNAPSHOT.jar").getMethod())
					.isEqualTo(ZipEntry.STORED);
		}
	}

	@Test
	void whenJarIsLayeredClasspathIndexPointsToLayeredLibs() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly(
					"- \"BOOT-INF/lib/first-library.jar\"", "- \"BOOT-INF/lib/second-library.jar\"",
					"- \"BOOT-INF/lib/third-library-SNAPSHOT.jar\"", "- \"BOOT-INF/lib/first-project-library.jar\"",
					"- \"BOOT-INF/lib/second-project-library-SNAPSHOT.jar\"");
		}
	}

	@Test
	void classpathIndexPointsToBootInfLibs() throws IOException {
		try (JarFile jarFile = new JarFile(createPopulatedJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classpath-Index"))
					.isEqualTo("BOOT-INF/classpath.idx");
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly(
					"- \"BOOT-INF/lib/first-library.jar\"", "- \"BOOT-INF/lib/second-library.jar\"",
					"- \"BOOT-INF/lib/third-library-SNAPSHOT.jar\"", "- \"BOOT-INF/lib/first-project-library.jar\"",
					"- \"BOOT-INF/lib/second-project-library-SNAPSHOT.jar\"");
		}
	}

	@Test
	void metaInfEntryIsPackagedInTheRootOfTheArchive() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File metaInfEntry = new File(classpathDirectory, "META-INF/test");
		metaInfEntry.getParentFile().mkdirs();
		metaInfEntry.createNewFile();
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		getTask().classpath(classpathDirectory);
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("BOOT-INF/classes/com/example/Application.class")).isNotNull();
			assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/META-INF/test")).isNull();
			assertThat(jarFile.getEntry("META-INF/test")).isNotNull();
		}
	}

	@Test
	void aopXmlIsPackagedBeneathClassesDirectory() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File aopXml = new File(classpathDirectory, "META-INF/aop.xml");
		aopXml.getParentFile().mkdirs();
		aopXml.createNewFile();
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		getTask().classpath(classpathDirectory);
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("BOOT-INF/classes/com/example/Application.class")).isNotNull();
			assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/META-INF/aop.xml")).isNotNull();
			assertThat(jarFile.getEntry("META-INF/aop.xml")).isNull();
		}
	}

	@Test
	void kotlinModuleIsPackagedBeneathClassesDirectory() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File kotlinModule = new File(classpathDirectory, "META-INF/example.kotlin_module");
		kotlinModule.getParentFile().mkdirs();
		kotlinModule.createNewFile();
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		getTask().classpath(classpathDirectory);
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("BOOT-INF/classes/com/example/Application.class")).isNotNull();
			assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/META-INF/example.kotlin_module")).isNotNull();
			assertThat(jarFile.getEntry("META-INF/example.kotlin_module")).isNull();
		}
	}

	@Test
	void metaInfServicesEntryIsPackagedBeneathClassesDirectory() throws IOException {
		getTask().getMainClass().set("com.example.Main");
		File classpathDirectory = new File(this.temp, "classes");
		File service = new File(classpathDirectory, "META-INF/services/com.example.Service");
		service.getParentFile().mkdirs();
		service.createNewFile();
		File applicationClass = new File(classpathDirectory, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		getTask().classpath(classpathDirectory);
		executeTask();
		try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getEntry("BOOT-INF/classes/com/example/Application.class")).isNotNull();
			assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
			assertThat(jarFile.getEntry("BOOT-INF/classes/META-INF/services/com.example.Service")).isNotNull();
			assertThat(jarFile.getEntry("META-INF/services/com.example.Service")).isNull();
		}

	}

	@Override
	void applyLayered(Action<LayeredSpec> action) {
		getTask().layered(action);
	}

	@Override
	void populateResolvedDependencies(Configuration configuration) {
		getTask().getResolvedDependencies().processConfiguration(getTask().getProject(), configuration);
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

}
