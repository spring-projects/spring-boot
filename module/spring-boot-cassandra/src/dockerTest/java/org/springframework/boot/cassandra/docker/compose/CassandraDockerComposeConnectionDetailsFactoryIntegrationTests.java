/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.docker.compose;

import java.util.List;

import org.springframework.boot.cassandra.autoconfigure.CassandraConnectionDetails;
import org.springframework.boot.cassandra.autoconfigure.CassandraConnectionDetails.Node;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CassandraDockerComposeConnectionDetailsFactory}.
 *
 * @author Scott Frederick
 */
class CassandraDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "cassandra-compose.yaml", image = TestImage.CASSANDRA)
	void runCreatesConnectionDetails(CassandraConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "cassandra-ssl-compose.yaml", image = TestImage.CASSANDRA, additionalResources = {
			"server-keystore.p12", "server-truststore.p12", "client-keystore.p12", "client-truststore.p12" })
	void runWithSslCreatesConnectionDetails(CassandraConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	private void assertConnectionDetails(CassandraConnectionDetails connectionDetails) {
		List<Node> contactPoints = connectionDetails.getContactPoints();
		assertThat(contactPoints).hasSize(1);
		Node node = contactPoints.get(0);
		assertThat(node.host()).isNotNull();
		assertThat(node.port()).isGreaterThan(0);
		assertThat(connectionDetails.getUsername()).isNull();
		assertThat(connectionDetails.getPassword()).isNull();
		assertThat(connectionDetails.getLocalDatacenter()).isEqualTo("testdc1");
	}

}
