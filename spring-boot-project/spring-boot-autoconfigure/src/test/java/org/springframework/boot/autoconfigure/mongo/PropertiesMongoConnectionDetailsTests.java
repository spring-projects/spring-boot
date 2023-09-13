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

package org.springframework.boot.autoconfigure.mongo;

import java.util.List;

import com.mongodb.ConnectionString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesMongoConnectionDetails}.
 *
 * @author Christoph Dreis
 * @author Scott Frederick
 */
class PropertiesMongoConnectionDetailsTests {

	private final MongoProperties properties = new MongoProperties();

	@Test
	void credentialsCanBeConfiguredWithUsername() {
		this.properties.setUsername("user");
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getUsername()).isEqualTo("user");
		assertThat(connectionString.getPassword()).isEmpty();
		assertThat(connectionString.getCredential().getUserName()).isEqualTo("user");
		assertThat(connectionString.getCredential().getPassword()).isEmpty();
		assertThat(connectionString.getCredential().getSource()).isEqualTo("test");
	}

	@Test
	void credentialsCanBeConfiguredWithUsernameAndPassword() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getUsername()).isEqualTo("user");
		assertThat(connectionString.getPassword()).isEqualTo("secret".toCharArray());
		assertThat(connectionString.getCredential().getUserName()).isEqualTo("user");
		assertThat(connectionString.getCredential().getPassword()).isEqualTo("secret".toCharArray());
		assertThat(connectionString.getCredential().getSource()).isEqualTo("test");
	}

	@Test
	void databaseCanBeConfigured() {
		this.properties.setDatabase("db");
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("db");
	}

	@Test
	void databaseHasDefaultWhenNotConfigured() {
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("test");
	}

	@Test
	void authenticationDatabaseCanBeConfigured() {
		this.properties.setUsername("user");
		this.properties.setDatabase("db");
		this.properties.setAuthenticationDatabase("authdb");
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("db");
		assertThat(connectionString.getCredential().getSource()).isEqualTo("authdb");
		assertThat(connectionString.getCredential().getUserName()).isEqualTo("user");
	}

	@Test
	void authenticationDatabaseIsNotConfiguredWhenUsernameIsNotConfigured() {
		this.properties.setAuthenticationDatabase("authdb");
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getCredential()).isNull();
	}

	@Test
	void replicaSetCanBeConfigured() {
		this.properties.setReplicaSetName("test");
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void replicaSetCanBeConfiguredWithDatabase() {
		this.properties.setUsername("user");
		this.properties.setDatabase("db");
		this.properties.setReplicaSetName("test");
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("db");
		assertThat(connectionString.getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void whenAdditionalHostsAreConfiguredThenTheyAreIncludedInHostsOfConnectionString() {
		this.properties.setHost("mongo1.example.com");
		this.properties.setAdditionalHosts(List.of("mongo2.example.com", "mongo3.example.com"));
		ConnectionString connectionString = getConnectionString();
		assertThat(connectionString.getHosts()).containsExactly("mongo1.example.com", "mongo2.example.com",
				"mongo3.example.com");
	}

	private PropertiesMongoConnectionDetails createConnectionDetails() {
		return new PropertiesMongoConnectionDetails(this.properties);
	}

	private ConnectionString getConnectionString() {
		PropertiesMongoConnectionDetails connectionDetails = createConnectionDetails();
		return connectionDetails.getConnectionString();
	}

}
