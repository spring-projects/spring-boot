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

package org.springframework.boot.docker.compose.service.connection.test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.function.ThrowingSupplier;

import static org.junit.Assert.fail;

/**
 * Abstract base class for integration tests.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@DisabledIfProcessUnavailable({ "docker", "version" })
@DisabledIfProcessUnavailable({ "docker", "compose" })
public abstract class AbstractDockerComposeIntegrationTests {

	@TempDir
	private static Path tempDir;

	private final Resource composeResource;

	private final DockerImageName dockerImageName;

	@AfterAll
	static void shutDown() {
		SpringApplicationShutdownHandlers shutdownHandlers = SpringApplication.getShutdownHandlers();
		((Runnable) shutdownHandlers).run();
	}

	protected AbstractDockerComposeIntegrationTests(String composeResource, DockerImageName dockerImageName) {
		this.composeResource = new ClassPathResource(composeResource, getClass());
		this.dockerImageName = dockerImageName;
	}

	protected final <T extends ConnectionDetails> T run(Class<T> type) {
		SpringApplication application = new SpringApplication(Config.class);
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("spring.docker.compose.skip.in-tests", "false");
		properties.put("spring.docker.compose.file",
				transformedComposeFile(ThrowingSupplier.of(this.composeResource::getFile).get(), this.dockerImageName));
		properties.put("spring.docker.compose.stop.command", "down");
		application.setDefaultProperties(properties);
		return application.run().getBean(type);
	}

	private File transformedComposeFile(File composeFile, DockerImageName imageName) {
		File tempComposeFile = Path.of(tempDir.toString(), composeFile.getName()).toFile();
		try {
			String composeFileContent;
			try (FileReader reader = new FileReader(composeFile)) {
				composeFileContent = FileCopyUtils.copyToString(reader);
			}
			composeFileContent = composeFileContent.replace("{imageName}", imageName.asCanonicalNameString());
			try (FileWriter writer = new FileWriter(tempComposeFile)) {
				FileCopyUtils.copy(composeFileContent, writer);
			}
		}
		catch (IOException ex) {
			fail("Error transforming Docker compose file '" + composeFile + "' to '" + tempComposeFile + "': "
					+ ex.getMessage());
		}
		return tempComposeFile;
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
