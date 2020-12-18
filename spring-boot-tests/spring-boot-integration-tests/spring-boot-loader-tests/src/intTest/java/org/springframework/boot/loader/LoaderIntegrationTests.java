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

package org.springframework.boot.loader;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests loader that supports fat jars.
 *
 * @author Phillip Webb
 */
@Testcontainers(disabledWithoutDocker = true)
class LoaderIntegrationTests {

	private static final DockerImageName JRE = DockerImageName.parse("adoptopenjdk:15-jre-hotspot");

	private static ToStringConsumer output = new ToStringConsumer();

	@Container
	public static GenericContainer<?> container = new GenericContainer<>(JRE).withLogConsumer(output)
			.withCopyFileToContainer(MountableFile.forHostPath(findApplication().toPath()), "/app.jar")
			.withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(5)))
			.withCommand("java", "-jar", "app.jar");

	private static File findApplication() {
		String name = String.format("build/%1$s/build/libs/%1$s.jar", "spring-boot-loader-tests-app");
		File jar = new File(name);
		Assert.state(jar.isFile(), () -> "Could not find " + name + ". Have you built it?");
		return jar;
	}

	@Test
	void readUrlsWithoutWarning() {
		System.out.println(output.toUtf8String());
		assertThat(output.toUtf8String()).contains(">>>>> 287649 BYTES from").doesNotContain("WARNING:")
				.doesNotContain("illegal").doesNotContain("jar written to temp");
	}

}
