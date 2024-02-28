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

package org.springframework.boot.docker.compose.service.connection.mongo;

import com.mongodb.ConnectionString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.boot.testsupport.testcontainers.BitnamiImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoDockerComposeConnectionDetailsFactory}.
 *
 * @author Scott Frederick
 */
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64", disabledReason = "The image has no ARM support")
class MongoBitnamiDockerComposeConnectionDetailsFactoryIntegrationTests extends AbstractDockerComposeIntegrationTests {

	MongoBitnamiDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("mongo-bitnami-compose.yaml", BitnamiImageNames.mongo());
	}

	@Test
	void runCreatesConnectionDetails() {
		MongoConnectionDetails connectionDetails = run(MongoConnectionDetails.class);
		ConnectionString connectionString = connectionDetails.getConnectionString();
		assertThat(connectionString.getCredential().getUserName()).isEqualTo("root");
		assertThat(connectionString.getCredential().getPassword()).isEqualTo("secret".toCharArray());
		assertThat(connectionString.getCredential().getSource()).isEqualTo("admin");
		assertThat(connectionString.getDatabase()).isEqualTo("test");
		assertThat(connectionDetails.getGridFs()).isNull();
	}

}
