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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Integration tests for the Maven plugin's AOT support.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
class AotTests {

	@TestTemplate
	void whenAotRunsSourcesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("sources")))
				.contains(Path.of("org", "test", "SampleApplication__ApplicationContextInitializer.java"));
		});
	}

	@TestTemplate
	void whenAotRunsResourcesAreGeneratedAndCopiedToTargetClasses(MavenBuild mavenBuild) {
		mavenBuild.project("aot-resource-generation").goals("package").execute((project) -> {
			Path targetClasses = project.toPath().resolve("target/classes");
			assertThat(collectRelativePaths(targetClasses)).contains(
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-resource-generation",
							"reflect-config.json"),
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-resource-generation",
							"resource-config.json"),
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-resource-generation",
							"native-image.properties"),
					Path.of("generated-resource"), Path.of("nested/generated-resource"));
		});
	}

	@TestTemplate
	void whenAotRunsWithJdkProxyResourcesIncludeProxyConfig(MavenBuild mavenBuild) {
		mavenBuild.project("aot-jdk-proxy").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("resources"))).contains(
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-jdk-proxy",
							"reflect-config.json"),
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-jdk-proxy",
							"resource-config.json"),
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-jdk-proxy",
							"proxy-config.json"),
					Path.of("META-INF", "native-image", "org.springframework.boot.maven.it", "aot-jdk-proxy",
							"native-image.properties"));
		});
	}

	@TestTemplate
	void whenAotRunsWithClassProxyClassesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot-class-proxy").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("classes")))
				.contains(Path.of("org", "test", "SampleRunner$$SpringCGLIB$$0.class"));
		});
	}

	@TestTemplate
	void whenAotRunsWithProfilesSourcesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot-profile").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("sources")))
				.contains(Path.of("org", "test", "TestProfileConfiguration__BeanDefinitions.java"));
		});
	}

	@TestTemplate
	void whenAotRunsWithArgumentsSourcesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot-arguments").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("sources")))
				.contains(Path.of("org", "test", "TestProfileConfiguration__BeanDefinitions.java"));
		});
	}

	@TestTemplate
	void whenAotRunsWithJvmArgumentsSourcesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot-jvm-arguments").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("sources")))
				.contains(Path.of("org", "test", "TestProfileConfiguration__BeanDefinitions.java"));
		});
	}

	@TestTemplate
	void whenAotRunsWithReleaseSourcesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot-release").goals("package").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/main");
			assertThat(collectRelativePaths(aotDirectory.resolve("sources")))
				.contains(Path.of("org", "test", "SampleApplication__ApplicationContextInitializer.java"));
		});
	}

	@TestTemplate
	void whenAotRunsWithInvalidCompilerArgumentsCompileFails(MavenBuild mavenBuild) {
		mavenBuild.project("aot-compiler-arguments")
			.goals("package")
			.executeAndFail(
					(project) -> assertThat(buildLog(project)).contains("invalid flag: --invalid-compiler-arg"));
	}

	@TestTemplate
	void whenAotRunsSourcesAreCompiled(MavenBuild mavenBuild) {
		mavenBuild.project("aot").goals("package").execute((project) -> {
			Path classesDirectory = project.toPath().resolve("target/classes");
			assertThat(collectRelativePaths(classesDirectory))
				.contains(Path.of("org", "test", "SampleApplication__ApplicationContextInitializer.class"));
		});
	}

	@TestTemplate
	void whenAotRunsResourcesAreCopiedToTargetClasses(MavenBuild mavenBuild) {
		mavenBuild.project("aot-jdk-proxy").goals("package").execute((project) -> {
			Path classesDirectory = project.toPath().resolve("target/classes/META-INF/native-image");
			assertThat(collectRelativePaths(classesDirectory)).contains(
					Path.of("org.springframework.boot.maven.it", "aot-jdk-proxy", "reflect-config.json"),
					Path.of("org.springframework.boot.maven.it", "aot-jdk-proxy", "resource-config.json"),
					Path.of("org.springframework.boot.maven.it", "aot-jdk-proxy", "proxy-config.json"),
					Path.of("org.springframework.boot.maven.it", "aot-jdk-proxy", "native-image.properties"));
		});
	}

	@TestTemplate
	void whenAotRunsWithClassProxyClassesAreCopiedToTargetClasses(MavenBuild mavenBuild) {
		mavenBuild.project("aot-class-proxy").goals("package").execute((project) -> {
			Path classesDirectory = project.toPath().resolve("target/classes/");
			assertThat(collectRelativePaths(classesDirectory))
				.contains(Path.of("org", "test", "SampleRunner$$SpringCGLIB$$0.class"));
		});
	}

	@TestTemplate
	void whenAotTestRunsSourcesAndResourcesAreGenerated(MavenBuild mavenBuild) {
		mavenBuild.project("aot-test").goals("test").execute((project) -> {
			Path aotDirectory = project.toPath().resolve("target/spring-aot/test");
			assertThat(collectRelativePaths(aotDirectory.resolve("sources"))).contains(Path.of("org", "test",
					"SampleApplicationTests__TestContext001_ApplicationContextInitializer.java"));
			Path testClassesDirectory = project.toPath().resolve("target/test-classes");
			assertThat(collectRelativePaths(testClassesDirectory)).contains(Path.of("META-INF", "native-image",
					"org.springframework.boot.maven.it", "aot-test", "reflect-config.json"));
			assertThat(collectRelativePaths(testClassesDirectory)).contains(Path.of("org", "test",
					"SampleApplicationTests__TestContext001_ApplicationContextInitializer.class"));
		});
	}

	List<Path> collectRelativePaths(Path sourceDirectory) {
		try (Stream<Path> pathStream = Files.walk(sourceDirectory)) {
			return pathStream.filter(Files::isRegularFile)
				.map((path) -> path.subpath(sourceDirectory.getNameCount(), path.getNameCount()))
				.toList();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected String buildLog(File project) {
		return contentOf(new File(project, "target/build.log"));
	}

}
