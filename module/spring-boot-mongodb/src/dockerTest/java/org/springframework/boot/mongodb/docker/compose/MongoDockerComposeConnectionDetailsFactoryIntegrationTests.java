/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.mongodb.docker.compose;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
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
