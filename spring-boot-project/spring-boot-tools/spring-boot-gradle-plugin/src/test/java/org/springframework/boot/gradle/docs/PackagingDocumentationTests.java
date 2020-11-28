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

package org.springframework.boot.gradle.docs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the packaging documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
class PackagingDocumentationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void warContainerDependencyEvaluatesSuccessfully() {
		this.gradleBuild.script("src/docs/gradle/packaging/war-container-dependency").build();
	}

	@TestTemplate
	void bootJarMainClass() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-main-class").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.ExampleApplication");
		}
	}

	@TestTemplate
	void bootJarManifestMainClass() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-manifest-main-class").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.ExampleApplication");
		}
	}

	@TestTemplate
	void applicationPluginMainClass() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/application-plugin-main-class").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.ExampleApplication");
		}
	}

	@TestTemplate
	void springBootDslMainClass() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/spring-boot-dsl-main-class").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.ExampleApplication");
		}
	}

	@TestTemplate
	void bootWarIncludeDevtools() throws IOException {
		jarFile(new File(this.gradleBuild.getProjectDir(), "spring-boot-devtools-1.2.3.RELEASE.jar"));
		this.gradleBuild.script("src/docs/gradle/packaging/boot-war-include-devtools").build("bootWar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".war");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			assertThat(jar.getEntry("WEB-INF/lib/spring-boot-devtools-1.2.3.RELEASE.jar")).isNotNull();
		}
	}

	@TestTemplate
	void bootJarRequiresUnpack() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-requires-unpack").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			JarEntry entry = jar.getJarEntry("BOOT-INF/lib/jruby-complete-1.7.25.jar");
			assertThat(entry).isNotNull();
			assertThat(entry.getComment()).startsWith("UNPACK:");
		}
	}

	@TestTemplate
	void bootJarIncludeLaunchScript() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-include-launch-script").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).startsWith("#!/bin/bash");
	}

	@TestTemplate
	void bootJarLaunchScriptProperties() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-launch-script-properties").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).contains("example-app.log");
	}

	@TestTemplate
	void bootJarCustomLaunchScript() throws IOException {
		File customScriptFile = new File(this.gradleBuild.getProjectDir(), "src/custom.script");
		customScriptFile.getParentFile().mkdirs();
		FileCopyUtils.copy("custom", new FileWriter(customScriptFile));
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-custom-launch-script").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).startsWith("custom");
	}

	@TestTemplate
	void bootWarPropertiesLauncher() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-war-properties-launcher").build("bootWar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".war");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			assertThat(jar.getManifest().getMainAttributes().getValue("Main-Class"))
					.isEqualTo("org.springframework.boot.loader.PropertiesLauncher");
		}
	}

	@TestTemplate
	void bootJarAndJar() {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-and-jar").build("assemble");
		File jar = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(jar).isFile();
		File bootJar = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + "-boot.jar");
		assertThat(bootJar).isFile();
	}

	@TestTemplate
	void bootJarLayeredDisabled() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-layered-disabled").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			JarEntry entry = jar.getJarEntry("BOOT-INF/layers.idx");
			assertThat(entry).isNull();
		}
	}

	@TestTemplate
	void bootJarLayeredCustom() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-layered-custom").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			JarEntry entry = jar.getJarEntry("BOOT-INF/layers.idx");
			assertThat(entry).isNotNull();
			assertThat(Collections.list(jar.entries()).stream().map(JarEntry::getName)
					.filter((name) -> name.startsWith("BOOT-INF/lib/spring-boot"))).isNotEmpty();
		}
	}

	@TestTemplate
	void bootJarLayeredExcludeTools() throws IOException {
		this.gradleBuild.script("src/docs/gradle/packaging/boot-jar-layered-exclude-tools").build("bootJar");
		File file = new File(this.gradleBuild.getProjectDir(),
				"build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(file).isFile();
		try (JarFile jar = new JarFile(file)) {
			JarEntry entry = jar.getJarEntry("BOOT-INF/layers.idx");
			assertThat(entry).isNotNull();
			assertThat(Collections.list(jar.entries()).stream().map(JarEntry::getName)
					.filter((name) -> name.startsWith("BOOT-INF/lib/spring-boot"))).isEmpty();
		}
	}

	@TestTemplate
	void bootBuildImageWithCustomBuildpackJvmVersion() throws IOException {
		BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/boot-build-image-env")
				.build("bootBuildImageEnvironment");
		assertThat(result.getOutput()).contains("BP_JVM_VERSION=8.*");
	}

	@TestTemplate
	void bootBuildImageWithCustomProxySettings() throws IOException {
		BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/boot-build-image-env-proxy")
				.build("bootBuildImageEnvironment");
		assertThat(result.getOutput()).contains("HTTP_PROXY=http://proxy.example.com")
				.contains("HTTPS_PROXY=https://proxy.example.com");
	}

	@TestTemplate
	void bootBuildImageWithCustomImageName() throws IOException {
		BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/boot-build-image-name")
				.build("bootBuildImageName");
		assertThat(result.getOutput()).contains("example.com/library/" + this.gradleBuild.getProjectDir().getName());
	}

	protected void jarFile(File file) throws IOException {
		try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
			jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			new Manifest().write(jar);
			jar.closeEntry();
		}
	}

}
