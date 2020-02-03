/*
 * Copyright 2012-2020 the original author or authors.
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

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoClientFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
class MongoClientFactoryTests {

	private MockEnvironment environment = new MockEnvironment();

	@Test
	void portCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setPort(12345);
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	void hostCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("mongo.example.com");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	void credentialsCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "test");
	}

	@Test
	void databaseCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "foo");
	}

	@Test
	void uuidRepresentationDefaultToJavaLegacy() {
		MongoProperties properties = new MongoProperties();
		MongoClient client = createMongoClient(properties);
		assertThat(getClientSettings(client).getUuidRepresentation()).isEqualTo(UuidRepresentation.JAVA_LEGACY);
	}

	@Test
	void uuidRepresentationCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUuidRepresentation(UuidRepresentation.STANDARD);
		MongoClient client = createMongoClient(properties);
		assertThat(getClientSettings(client).getUuidRepresentation()).isEqualTo(UuidRepresentation.STANDARD);
	}

	@Test
	void authenticationDatabaseCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setAuthenticationDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "foo");
	}

	@Test
	void uriCanBeCustomized() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://user:secret@mongo1.example.com:12345,mongo2.example.com:23456/test");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "test");
	}

	@Test
	void uriIsIgnoredInEmbeddedMode() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://mongo.example.com:1234/mydb");
		this.environment.setProperty("local.mongo.port", "4000");
		MongoClient client = createMongoClient(properties, this.environment);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 4000);
	}

	private MongoClient createMongoClient(MongoProperties properties) {
		return createMongoClient(properties, null);
	}

	private MongoClient createMongoClient(MongoProperties properties, Environment environment) {
		return new MongoClientFactory(properties, environment).createMongoClient(null);
	}

	private List<ServerAddress> getAllAddresses(MongoClient client) {
		return client.getClusterDescription().getClusterSettings().getHosts();
	}

	private MongoClientSettings getClientSettings(MongoClient client) {
		return (MongoClientSettings) ReflectionTestUtils.getField(client, "settings");
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
