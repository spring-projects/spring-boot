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

import java.util.Collections;
import java.util.Map;

import com.mongodb.ConnectionString;
import com.mongodb.MongoCredential;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.mongodb.docker.compose.MongoDockerComposeConnectionDetailsFactory.MongoDockerComposeConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoDockerComposeConnectionDetailsFactory}.
 *
 * @author Iram Tazim Hoque
 */
class MongoDockerComposeConnectionDetailsFactoryTests {

	@Test
	void createConnectionDetails() {
		RunningService runningService = mockRunningService();
		given(runningService.env()).willReturn(Map.of("MONGO_INITDB_ROOT_USERNAME", "root",
				"MONGO_INITDB_ROOT_PASSWORD", "secret", "MONGO_INITDB_DATABASE", "mydatabase"));
		ConnectionString connectionString = new MongoDockerComposeConnectionDetails(runningService)
			.getConnectionString();
		assertThat(connectionString.getHosts()).containsExactly("localhost:30001");
		assertThat(connectionString.getDatabase()).isEqualTo("mydatabase");
		MongoCredential credential = connectionString.getCredential();
		assertThat(credential).isNotNull();
		assertThat(credential.getUserName()).isEqualTo("root");
		assertThat(credential.getPassword()).isEqualTo("secret".toCharArray());
		assertThat(credential.getSource()).isEqualTo("admin");
	}

	@Test
	void createConnectionDetailsWhenCredentialsContainReservedCharacters() {
		RunningService runningService = mockRunningService();
		given(runningService.env()).willReturn(Map.of("MONGO_INITDB_ROOT_USERNAME", "us@r:name",
				"MONGO_INITDB_ROOT_PASSWORD", "p@ss:w/rd%?#[] +", "MONGO_INITDB_DATABASE", "mydatabase"));
		ConnectionString connectionString = new MongoDockerComposeConnectionDetails(runningService)
			.getConnectionString();
		assertThat(connectionString.getHosts()).containsExactly("localhost:30001");
		assertThat(connectionString.getDatabase()).isEqualTo("mydatabase");
		MongoCredential credential = connectionString.getCredential();
		assertThat(credential).isNotNull();
		assertThat(credential.getUserName()).isEqualTo("us@r:name");
		assertThat(credential.getPassword()).isEqualTo("p@ss:w/rd%?#[] +".toCharArray());
		assertThat(credential.getSource()).isEqualTo("admin");
	}

	@Test
	void createConnectionDetailsWithoutCredentials() {
		RunningService runningService = mockRunningService();
		given(runningService.env()).willReturn(Collections.emptyMap());
		ConnectionString connectionString = new MongoDockerComposeConnectionDetails(runningService)
			.getConnectionString();
		assertThat(connectionString.getHosts()).containsExactly("localhost:30001");
		assertThat(connectionString.getDatabase()).isEqualTo("test");
		assertThat(connectionString.getCredential()).isNull();
	}

	private static RunningService mockRunningService() {
		RunningService service = mock(RunningService.class);
		given(service.labels()).willReturn(Collections.emptyMap());
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(service.ports()).willReturn(connectionPorts);
		given(service.host()).willReturn("localhost");
		given(connectionPorts.get(27017)).willReturn(30001);
		return service;
	}

}
