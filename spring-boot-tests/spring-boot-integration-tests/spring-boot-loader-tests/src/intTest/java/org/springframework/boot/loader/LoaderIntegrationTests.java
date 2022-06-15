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

package org.springframework.boot.loader;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.system.JavaVersion;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests loader that supports fat jars.
 *
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
class LoaderIntegrationTests {

	private final ToStringConsumer output = new ToStringConsumer();

	@ParameterizedTest
	@MethodSource("javaRuntimes")
	void readUrlsWithoutWarning(JavaRuntime javaRuntime) {
		try (GenericContainer<?> container = createContainer(javaRuntime, "spring-boot-loader-tests-app")) {
			container.start();
			System.out.println(this.output.toUtf8String());
			assertThat(this.output.toUtf8String()).contains(">>>>> 287649 BYTES from").doesNotContain("WARNING:")
					.doesNotContain("illegal").doesNotContain("jar written to temp");
		}
	}

	@ParameterizedTest
	@MethodSource("javaRuntimes")
	void runSignedJarWhenUnpacked(JavaRuntime javaRuntime) {
		try (GenericContainer<?> container = createContainer(javaRuntime,
				"spring-boot-loader-tests-signed-jar-unpack-app")) {
			container.start();
			System.out.println(this.output.toUtf8String());
			assertThat(this.output.toUtf8String()).contains("Legion of the Bouncy Castle");
		}
	}

	private GenericContainer<?> createContainer(JavaRuntime javaRuntime, String name) {
		return javaRuntime.getContainer().withLogConsumer(this.output)
				.withCopyFileToContainer(findApplication(name), "/app.jar")
				.withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(5)))
				.withCommand("java", "-jar", "app.jar");
	}

	private MountableFile findApplication(String name) {
		return MountableFile.forHostPath(findJarFile(name).toPath());
	}

	private File findJarFile(String name) {
		String path = String.format("build/%1$s/build/libs/%1$s.jar", name);
		File jar = new File(path);
		Assert.state(jar.isFile(), () -> "Could not find " + path + ". Have you built it?");
		return jar;
	}

	static Stream<JavaRuntime> javaRuntimes() {
		List<JavaRuntime> javaRuntimes = new ArrayList<>();
		javaRuntimes.add(JavaRuntime.openJdk(JavaVersion.SEVENTEEN));
		javaRuntimes.add(JavaRuntime.openJdk(JavaVersion.EIGHTEEN));
		javaRuntimes.add(JavaRuntime.oracleJdk17());
		return javaRuntimes.stream().filter(JavaRuntime::isCompatible);
	}

	static final class JavaRuntime {

		private final String name;

		private final JavaVersion version;

		private final Supplier<GenericContainer<?>> container;

		private JavaRuntime(String name, JavaVersion version, Supplier<GenericContainer<?>> container) {
			this.name = name;
			this.version = version;
			this.container = container;
		}

		private boolean isCompatible() {
			return this.version.isEqualOrNewerThan(JavaVersion.getJavaVersion());
		}

		GenericContainer<?> getContainer() {
			return this.container.get();
		}

		@Override
		public String toString() {
			return this.name;
		}

		static JavaRuntime openJdk(JavaVersion version) {
			String imageVersion = version.toString();
			DockerImageName image = DockerImageName.parse("bellsoft/liberica-openjdk-debian:" + imageVersion);
			return new JavaRuntime("OpenJDK " + imageVersion, version, () -> new GenericContainer<>(image));
		}

		static JavaRuntime oracleJdk17() {
			ImageFromDockerfile image = new ImageFromDockerfile("spring-boot-loader/oracle-jdk-17")
					.withFileFromFile("Dockerfile", new File("src/intTest/resources/conf/oracle-jdk-17/Dockerfile"));
			return new JavaRuntime("Oracle JDK 17", JavaVersion.SEVENTEEN, () -> new GenericContainer<>(image));
		}

	}

}
