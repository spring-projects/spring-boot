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

package org.springframework.boot.docker.compose.service.connection.zipkin;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ZipkinDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ZipkinDockerComposeConnectionDetailsFactoryIntegrationTests extends AbstractDockerComposeIntegrationTests {

	ZipkinDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("zipkin-compose.yaml", DockerImageNames.zipkin());
	}

	@Test
	void runCreatesConnectionDetails() {
		ZipkinConnectionDetails connectionDetails = run(ZipkinConnectionDetails.class);
		assertThat(connectionDetails.getSpanEndpoint()).startsWith("http://").endsWith("/api/v2/spans");
	}

}
