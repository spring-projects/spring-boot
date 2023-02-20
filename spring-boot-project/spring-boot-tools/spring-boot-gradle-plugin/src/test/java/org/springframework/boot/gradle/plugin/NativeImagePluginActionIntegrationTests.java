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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import org.springframework.boot.gradle.junit.GradleCompatibility;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NativeImagePluginAction}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@GradleCompatibility(configurationCache = false)
class NativeImagePluginActionIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void applyingNativeImagePluginAppliesAotPlugin() {
		assertThat(this.gradleBuild.build("aotPluginApplied").getOutput())
				.contains("org.springframework.boot.aot applied = true");
	}

	@TestTemplate
	void reachabilityMetadataConfigurationFilesAreCopiedToJar() throws IOException {
		writeDummySpringApplicationAotProcessorMainClass();
		BuildResult result = this.gradleBuild.build("bootJar");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		File jarFile = new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(buildLibs.listFiles()).contains(jarFile);
		assertThat(getEntryNames(jarFile)).contains(
				"META-INF/native-image/ch.qos.logback/logback-classic/1.2.11/reflect-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/jni-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/proxy-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/reflect-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/resource-config.json");
	}

	@TestTemplate
	void reachabilityMetadataConfigurationFilesFromFileRepositoryAreCopiedToJar() throws IOException {
		writeDummySpringApplicationAotProcessorMainClass();
		FileSystemUtils.copyRecursively(new File("src/test/resources/reachability-metadata-repository"),
				new File(this.gradleBuild.getProjectDir(), "reachability-metadata-repository"));
		BuildResult result = this.gradleBuild.build("bootJar");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		File jarFile = new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar");
		assertThat(buildLibs.listFiles()).contains(jarFile);
		assertThat(getEntryNames(jarFile)).contains(
				"META-INF/native-image/ch.qos.logback/logback-classic/1.2.11/reflect-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/jni-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/proxy-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/reflect-config.json",
				"META-INF/native-image/org.jline/jline/3.21.0/resource-config.json");
	}

	@TestTemplate
	void bootBuildImageIsConfiguredToBuildANativeImage() {
		writeDummySpringApplicationAotProcessorMainClass();
		BuildResult result = this.gradleBuild.build("bootBuildImageConfiguration");
		assertThat(result.getOutput()).contains("paketobuildpacks/builder:tiny").contains("BP_NATIVE_IMAGE = true");
	}

	@TestTemplate
	void developmentOnlyDependenciesDoNotAppearInNativeImageClasspath() {
		writeDummySpringApplicationAotProcessorMainClass();
		BuildResult result = this.gradleBuild.build("checkNativeImageClasspath");
		assertThat(result.getOutput()).doesNotContain("commons-lang");
	}

	@TestTemplate
	void classesGeneratedDuringAotProcessingAreOnTheNativeImageClasspath() {
		BuildResult result = this.gradleBuild.build("checkNativeImageClasspath");
		assertThat(result.getOutput()).contains(projectPath("build/classes/java/aot"),
				projectPath("build/resources/aot"), projectPath("build/generated/aotClasses"));
	}

	@TestTemplate
	void classesGeneratedDuringAotTestProcessingAreOnTheTestNativeImageClasspath() {
		BuildResult result = this.gradleBuild.build("checkTestNativeImageClasspath");
		assertThat(result.getOutput()).contains(projectPath("build/classes/java/aotTest"),
				projectPath("build/resources/aotTest"), projectPath("build/generated/aotTestClasses"));
	}

	@TestTemplate
	void nativeImageBinariesRequireGraal22Dot3() {
		BuildResult result = this.gradleBuild.build("requiredGraalVersion");
		assertThat(result.getOutput()).contains("custom: 22.3", "main: 22.3", "test: 22.3");
	}

	private String projectPath(String path) {
		try {
			return new File(this.gradleBuild.getProjectDir(), path).getCanonicalPath();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeDummySpringApplicationAotProcessorMainClass() {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/org/springframework/boot");
		examplePackage.mkdirs();
		File main = new File(examplePackage, "SpringApplicationAotProcessor.java");
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			writer.println("package org.springframework.boot;");
			writer.println();
			writer.println("import java.io.IOException;");
			writer.println();
			writer.println("public class SpringApplicationAotProcessor {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected List<String> getEntryNames(File file) throws IOException {
		List<String> entryNames = new ArrayList<>();
		try (JarFile jarFile = new JarFile(file)) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				entryNames.add(entries.nextElement().getName());
			}
		}
		return entryNames;
	}

}
