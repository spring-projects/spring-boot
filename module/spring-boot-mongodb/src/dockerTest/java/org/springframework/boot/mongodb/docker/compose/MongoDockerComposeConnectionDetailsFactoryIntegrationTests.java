/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.docker.compose;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MongoDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "mongo-compose.yaml", image = TestImage.MONGODB)
	void runCreatesConnectionDetails(MongoConnectionDetails connectionDetails) {
		assertConnectionDetailsWithDatabase(connectionDetails, "mydatabase");
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "mongo-ssl-compose.yaml", image = TestImage.MONGODB,
			additionalResources = { "ca.crt", "client.crt", "client.key", "mongo.pem" })
	void runWithSslCreatesConnectionDetails(MongoConnectionDetails connectionDetails) {
		assertConnectionDetailsWithDatabase(connectionDetails, "mydatabase");
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	private void assertConnectionDetailsWithDatabase(MongoConnectionDetails connectionDetails, String database) {
		ConnectionString connectionString = connectionDetails.getConnectionString();
		MongoCredential credential = connectionString.getCredential();
		assertThat(credential).isNotNull();
		assertThat(credential.getUserName()).isEqualTo("root");
		assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
		assertThat(credential.getSource()).isEqualTo("admin");
		assertThat(connectionString.getDatabase()).isEqualTo(database);
	}

}
