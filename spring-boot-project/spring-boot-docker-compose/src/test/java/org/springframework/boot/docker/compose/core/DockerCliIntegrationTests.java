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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeConfig;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeDown;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposePs;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeStart;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeStop;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeUp;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Inspect;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
@DisabledIfProcessUnavailable({ "docker", "compose" })
class DockerCliIntegrationTests {

	@TempDir
	private static Path tempDir;

	@Test
	void runBasicCommand() {
		DockerCli cli = new DockerCli(null, null, Collections.emptySet());
		List<DockerCliContextResponse> context = cli.run(new DockerCliCommand.Context());
		assertThat(context).isNotEmpty();
	}

	@Test
	void runLifecycle() throws IOException {
		File composeFile = createComposeFile();
		DockerCli cli = new DockerCli(null, DockerComposeFile.of(composeFile), Collections.emptySet());
		try {
			// Verify that no services are running (this is a fresh compose project)
			List<DockerCliComposePsResponse> ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
			// List the config and verify that redis is there
			DockerCliComposeConfigResponse config = cli.run(new ComposeConfig());
			assertThat(config.services()).containsOnlyKeys("redis");
			// Run up
			cli.run(new ComposeUp(LogLevel.INFO));
			// Run ps and use id to run inspect on the id
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
			String id = ps.get(0).id();
			List<DockerCliInspectResponse> inspect = cli.run(new Inspect(List.of(id)));
			assertThat(inspect).isNotEmpty();
			assertThat(inspect.get(0).id()).isEqualTo(id);
			// Run stop, then run ps and verify the services are stopped
			cli.run(new ComposeStop(Duration.ofSeconds(10)));
			ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
			// Run start, verify service is there, then run down and verify they are gone
			cli.run(new ComposeStart(LogLevel.INFO));
			ps = cli.run(new ComposePs());
			assertThat(ps).hasSize(1);
			cli.run(new ComposeDown(Duration.ofSeconds(10)));
			ps = cli.run(new ComposePs());
			assertThat(ps).isEmpty();
		}
		finally {
			// Clean up in any case
			quietComposeDown(cli);
		}
	}

	private static void quietComposeDown(DockerCli cli) {
		try {
			cli.run(new ComposeDown(Duration.ZERO));
		}
		catch (RuntimeException ex) {
			// Ignore
		}
	}

	private static File createComposeFile() throws IOException {
		File composeFile = new ClassPathResource("redis-compose.yaml", DockerCliIntegrationTests.class).getFile();
		File tempComposeFile = Path.of(tempDir.toString(), composeFile.getName()).toFile();
		String composeFileContent = FileCopyUtils.copyToString(new FileReader(composeFile));
		composeFileContent = composeFileContent.replace("{imageName}",
				DockerImageNames.redis().asCanonicalNameString());
		FileCopyUtils.copy(composeFileContent, new FileWriter(tempComposeFile));
		return tempComposeFile;
	}

}
