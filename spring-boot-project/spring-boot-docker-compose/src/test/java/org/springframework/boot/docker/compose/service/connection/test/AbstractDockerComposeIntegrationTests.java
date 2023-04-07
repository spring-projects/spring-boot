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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Abstract base class for integration tests.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
@DisabledIfProcessUnavailable({ "docker", "compose" })
public abstract class AbstractDockerComposeIntegrationTests {

	private final Resource composeResource;

	@AfterAll
	static void shutdown() {
		SpringApplicationShutdownHandlers shutdownHandlers = SpringApplication.getShutdownHandlers();
		((Runnable) shutdownHandlers).run();
	}

	protected AbstractDockerComposeIntegrationTests(String composeResource) {
		this.composeResource = new ClassPathResource(composeResource, getClass());
	}

	protected final <T extends ConnectionDetails> T run(Class<T> type) {
		SpringApplication application = new SpringApplication(Config.class);
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("spring.docker.compose.skip.in-tests", "false");
		properties.put("spring.docker.compose.file",
				ThrowingSupplier.of(this.composeResource::getFile).get().getAbsolutePath());
		application.setDefaultProperties(properties);
		return application.run().getBean(type);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
