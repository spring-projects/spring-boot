/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ReactiveMongoClientFactory}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class ReactiveMongoClientFactoryTests {

	private MockEnvironment environment = new MockEnvironment();

	@Test
	void portCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setPort(12345);
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	void hostCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("mongo.example.com");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	void credentialsCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(extractMongoCredentials(client), "user", "secret", "test");
	}

	@Test
	void databaseCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(extractMongoCredentials(client), "user", "secret", "foo");
	}

	@Test
	void authenticationDatabaseCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setAuthenticationDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(extractMongoCredentials(client), "user", "secret", "foo");
	}

	@Test
	void uriCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://user:secret@mongo1.example.com:12345,mongo2.example.com:23456/test");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		MongoCredential credential = extractMongoCredentials(client);
		assertMongoCredential(credential, "user", "secret", "test");
	}

	@Test
	void retryWritesIsPropagatedFromUri() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://localhost/test?retryWrites=true");
		MongoClient client = createMongoClient(properties);
		assertThat(getSettings(client).getRetryWrites()).isTrue();
	}

	@Test
	void uriCannotBeSetWithCredentials() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://127.0.0.1:1234/mydb");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		assertThatIllegalStateException().isThrownBy(() -> createMongoClient(properties)).withMessageContaining(
				"Invalid mongo configuration, either uri or host/port/credentials must be specified");
	}

	@Test
	void uriCannotBeSetWithHostPort() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://127.0.0.1:1234/mydb");
		properties.setHost("localhost");
		properties.setPort(4567);
		assertThatIllegalStateException().isThrownBy(() -> createMongoClient(properties)).withMessageContaining(
				"Invalid mongo configuration, either uri or host/port/credentials must be specified");
	}

	@Test
	void uriIsIgnoredInEmbeddedMode() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://mongo.example.com:1234/mydb");
		this.environment.setProperty("local.mongo.port", "4000");
		MongoClient client = createMongoClient(properties, this.environment);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 4000);
	}

	@Test
	void customizerIsInvoked() {
		MongoProperties properties = new MongoProperties();
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(properties, this.environment, customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	@Test
	void customizerIsInvokedWhenHostIsSet() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("localhost");
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(properties, this.environment, customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	@Test
	void customizerIsInvokedForEmbeddedMongo() {
		MongoProperties properties = new MongoProperties();
		this.environment.setProperty("local.mongo.port", "27017");
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(properties, this.environment, customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	private MongoClient createMongoClient(MongoProperties properties) {
		return createMongoClient(properties, this.environment);
	}

	private MongoClient createMongoClient(MongoProperties properties, Environment environment,
			MongoClientSettingsBuilderCustomizer... customizers) {
		return new ReactiveMongoClientFactory(properties, environment, Arrays.asList(customizers))
				.createMongoClient(null);
	}

	private List<ServerAddress> extractServerAddresses(MongoClient client) {
		MongoClientSettings settings = getSettings(client);
		ClusterSettings clusterSettings = settings.getClusterSettings();
		return clusterSettings.getHosts();
	}

	private MongoCredential extractMongoCredentials(MongoClient client) {
		return getSettings(client).getCredential();
	}

	@SuppressWarnings("deprecation")
	private MongoClientSettings getSettings(MongoClient client) {
		return (MongoClientSettings) ReflectionTestUtils.getField(client.getSettings(), "wrapped");
	}

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost, int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	private void assertMongoCredential(MongoCredential credentials, String expectedUsername, String expectedPassword,
			String expectedSource) {
		assertThat(credentials.getUserName()).isEqualTo(expectedUsername);
		assertThat(credentials.getPassword()).isEqualTo(expectedPassword.toCharArray());
		assertThat(credentials.getSource()).isEqualTo(expectedSource);
	}

}
