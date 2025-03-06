/*
 * Copyright 2012-2025 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertiesMongoConnectionDetails}.
 *
 * @author Christoph Dreis
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PropertiesMongoConnectionDetailsTests {

	private MongoProperties properties;

	private DefaultSslBundleRegistry sslBundleRegistry;

	private PropertiesMongoConnectionDetails connectionDetails;

	@BeforeEach
	void setUp() {
		this.properties = new MongoProperties();
		this.sslBundleRegistry = new DefaultSslBundleRegistry();
		this.connectionDetails = new PropertiesMongoConnectionDetails(this.properties, this.sslBundleRegistry);
	}

	@Test
	void credentialsCanBeConfiguredWithUsername() {
		this.properties.setUsername("user");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
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
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getUsername()).isEqualTo("user");
		assertThat(connectionString.getPassword()).isEqualTo("secret".toCharArray());
		assertThat(connectionString.getCredential().getUserName()).isEqualTo("user");
		assertThat(connectionString.getCredential().getPassword()).isEqualTo("secret".toCharArray());
		assertThat(connectionString.getCredential().getSource()).isEqualTo("test");
	}

	@Test
	void databaseCanBeConfigured() {
		this.properties.setDatabase("db");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("db");
	}

	@Test
	void databaseHasDefaultWhenNotConfigured() {
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("test");
	}

	@Test
	void protocolCanBeConfigured() {
		this.properties.setProtocol("mongodb+srv");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getConnectionString()).startsWith("mongodb+srv://");
	}

	@Test
	void authenticationDatabaseCanBeConfigured() {
		this.properties.setUsername("user");
		this.properties.setDatabase("db");
		this.properties.setAuthenticationDatabase("authdb");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("db");
		assertThat(connectionString.getCredential().getSource()).isEqualTo("authdb");
		assertThat(connectionString.getCredential().getUserName()).isEqualTo("user");
	}

	@Test
	void authenticationDatabaseIsNotConfiguredWhenUsernameIsNotConfigured() {
		this.properties.setAuthenticationDatabase("authdb");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getCredential()).isNull();
	}

	@Test
	void replicaSetCanBeConfigured() {
		this.properties.setReplicaSetName("test");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void replicaSetCanBeConfiguredWithDatabase() {
		this.properties.setUsername("user");
		this.properties.setDatabase("db");
		this.properties.setReplicaSetName("test");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getDatabase()).isEqualTo("db");
		assertThat(connectionString.getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void replicaSetCanBeNull() {
		this.properties.setReplicaSetName(null);
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getRequiredReplicaSetName()).isNull();
	}

	@Test
	void replicaSetCanBeBlank() {
		this.properties.setReplicaSetName("");
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getRequiredReplicaSetName()).isNull();
	}

	@Test
	void whenAdditionalHostsAreConfiguredThenTheyAreIncludedInHostsOfConnectionString() {
		this.properties.setHost("mongo1.example.com");
		this.properties.setAdditionalHosts(List.of("mongo2.example.com", "mongo3.example.com"));
		ConnectionString connectionString = this.connectionDetails.getConnectionString();
		assertThat(connectionString.getHosts()).containsExactly("mongo1.example.com", "mongo2.example.com",
				"mongo3.example.com");
	}

	@Test
	void shouldReturnSslBundle() {
		SslBundle bundle1 = mock(SslBundle.class);
		this.sslBundleRegistry.registerBundle("bundle-1", bundle1);
		this.properties.getSsl().setBundle("bundle-1");
		SslBundle sslBundle = this.connectionDetails.getSslBundle();
		assertThat(sslBundle).isSameAs(bundle1);
	}

	@Test
	void shouldReturnSystemDefaultBundleIfSslIsEnabledButBundleNotSet() {
		this.properties.getSsl().setEnabled(true);
		SslBundle sslBundle = this.connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	@Test
	void shouldReturnNullIfSslIsNotEnabled() {
		this.properties.getSsl().setEnabled(false);
		SslBundle sslBundle = this.connectionDetails.getSslBundle();
		assertThat(sslBundle).isNull();
	}

}
