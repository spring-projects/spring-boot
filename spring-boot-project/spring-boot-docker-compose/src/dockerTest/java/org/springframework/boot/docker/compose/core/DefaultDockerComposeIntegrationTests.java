/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.docker.compose.core.DockerCli.DockerComposeOptions;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultDockerCompose}.
 *
 * @author Moritz Halbritter
 */
@DisabledIfDockerUnavailable
@DisabledIfProcessUnavailable({ "docker", "compose" })
class DefaultDockerComposeIntegrationTests {

	@Test
	void shouldWorkWithProfiles(@TempDir Path tempDir) throws IOException {
		// Profile 1 contains redis1 and redis3
		// Profile 2 contains redis2 and redis3
		File composeFile = createComposeFile(tempDir, "profiles.yaml").toFile();
		DefaultDockerCompose dockerComposeWithProfile1 = new DefaultDockerCompose(new DockerCli(tempDir.toFile(),
				new DockerComposeOptions(DockerComposeFile.of(composeFile), Set.of("1"), Collections.emptyList())),
				null);
		DefaultDockerCompose dockerComposeWithProfile2 = new DefaultDockerCompose(new DockerCli(tempDir.toFile(),
				new DockerComposeOptions(DockerComposeFile.of(composeFile), Set.of("2"), Collections.emptyList())),
				null);
		DefaultDockerCompose dockerComposeWithAllProfiles = new DefaultDockerCompose(new DockerCli(tempDir.toFile(),
				new DockerComposeOptions(DockerComposeFile.of(composeFile), Set.of("1", "2"), Collections.emptyList())),
				null);
		dockerComposeWithAllProfiles.up(LogLevel.DEBUG);
		try {
			List<RunningService> runningServicesProfile1 = dockerComposeWithProfile1.getRunningServices();
			assertThatContainsService(runningServicesProfile1, "redis1");
			assertThatDoesNotContainService(runningServicesProfile1, "redis2");
			assertThatContainsService(runningServicesProfile1, "redis3");

			List<RunningService> runningServicesProfile2 = dockerComposeWithProfile2.getRunningServices();
			assertThatDoesNotContainService(runningServicesProfile2, "redis1");
			assertThatContainsService(runningServicesProfile2, "redis2");
			assertThatContainsService(runningServicesProfile2, "redis3");

			// Assert that redis3 is started only once and is shared between profile 1 and
			// profile 2
			assertThat(dockerComposeWithAllProfiles.getRunningServices()).hasSize(3);
			RunningService redis3Profile1 = findService(runningServicesProfile1, "redis3");
			RunningService redis3Profile2 = findService(runningServicesProfile2, "redis3");
			assertThat(redis3Profile1).isNotNull();
			assertThat(redis3Profile2).isNotNull();
			assertThat(redis3Profile1.name()).isEqualTo(redis3Profile2.name());
		}
		finally {
			dockerComposeWithAllProfiles.down(Duration.ofSeconds(10));
		}
	}

	private RunningService findService(List<RunningService> runningServices, String serviceName) {
		for (RunningService runningService : runningServices) {
			if (runningService.name().contains(serviceName)) {
				return runningService;
			}
		}
		return null;
	}

	private void assertThatDoesNotContainService(List<RunningService> runningServices, String service) {
		if (findService(runningServices, service) != null) {
			Assertions.fail("Did not expect service '%s', but found it in [%s]", service, runningServices);
		}
	}

	private void assertThatContainsService(List<RunningService> runningServices, String service) {
		if (findService(runningServices, service) == null) {
			Assertions.fail("Expected service '%s', but hasn't been found in [%s]", service, runningServices);
		}
	}

	private static Path createComposeFile(Path tempDir, String resource) throws IOException {
		String composeFileTemplate = new ClassPathResource(resource, DockerCliIntegrationTests.class)
			.getContentAsString(StandardCharsets.UTF_8);
		String content = composeFileTemplate.replace("{imageName}", TestImage.REDIS.toString());
		Path composeFile = tempDir.resolve(resource);
		Files.writeString(composeFile, content);
		return composeFile;
	}

}
