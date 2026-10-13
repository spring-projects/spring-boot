/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.zipkin.autoconfigure.ZipkinConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ZipkinDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ZipkinDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "zipkin-compose.yaml", image = TestImage.ZIPKIN)
	void runCreatesConnectionDetails(ZipkinConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getSpanEndpoint()).startsWith("http://").endsWith("/api/v2/spans");
	}

}
