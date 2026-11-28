/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.mail.autoconfigure.MailConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MailpitDockerComposeConnectionDetailsFactory}.
 *
 * @author Dominik Kovacs
 */
class MailpitDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "mailpit-compose.yaml", image = TestImage.MAILPIT)
	void runCreatesConnectionDetails(MailConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getHost()).isNotNull();
		assertThat(connectionDetails.getPort()).isGreaterThan(0);
		assertThat(connectionDetails.getUsername()).isNull();
		assertThat(connectionDetails.getPassword()).isNull();
	}

}
