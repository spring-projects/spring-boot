/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.boot.console;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.system.JavaVersion;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that checks ANSI output is not turned on in JDK 22 or later.
 *
 * @author Yong-Hyun Kim
 */
@DisabledIfDockerUnavailable
class ConsoleIntegrationTests {

	private static final String ENCODE_START = "\033[";

	private static final JavaRuntime JDK_17_RUNTIME = JavaRuntime.openJdk(JavaVersion.SEVENTEEN);

	private static final JavaRuntime JDK_22_RUNTIME = JavaRuntime.openJdk(JavaVersion.TWENTY_TWO);

	private final ToStringConsumer output = new ToStringConsumer().withRemoveAnsiCodes(false);

	@Test
	void runJarIn17() {
		try (GenericContainer<?> container = createContainer(JDK_17_RUNTIME)) {
			container.start();
			assertThat(this.output.toString(StandardCharsets.ISO_8859_1)).contains("System.console() is null")
				.doesNotContain(ENCODE_START);
		}
	}

	@Test
	void runJarIn22() {
		try (GenericContainer<?> container = createContainer(JDK_22_RUNTIME)) {
			container.start();
			assertThat(this.output.toString(StandardCharsets.ISO_8859_1)).doesNotContain("System.console() is null")
				.doesNotContain(ENCODE_START);
		}
	}

	private GenericContainer<?> createContainer(JavaRuntime javaRuntime) {
		return javaRuntime.getContainer()
			.withLogConsumer(this.output)
			.withCopyFileToContainer(findApplication(), "/app.jar")
			.withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(5)))
			.withCommand("java", "-jar", "app.jar");
	}

	private MountableFile findApplication() {
		return MountableFile.forHostPath(findJarFile().toPath());
	}

	private File findJarFile() {
		String path = String.format("build/%1$s/build/libs/%1$s.jar", "spring-boot-console-tests-app");
		File jar = new File(path);
		Assert.state(jar.isFile(), () -> "Could not find " + path + ". Have you built it?");
		return jar;
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

	}

}
