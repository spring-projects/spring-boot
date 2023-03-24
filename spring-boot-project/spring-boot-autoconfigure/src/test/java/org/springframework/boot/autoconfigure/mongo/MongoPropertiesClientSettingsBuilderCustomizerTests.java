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

import java.util.Arrays;
import java.util.List;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoPropertiesClientSettingsBuilderCustomizer}.
 *
 * @author Scott Frederick
 */
@Deprecated(since = "3.1.0", forRemoval = true)
class MongoPropertiesClientSettingsBuilderCustomizerTests {

	private final MongoProperties properties = new MongoProperties();

	@Test
	void portCanBeCustomized() {
		this.properties.setPort(12345);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	void hostCanBeCustomized() {
		this.properties.setHost("mongo.example.com");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	void additionalHostCanBeAdded() {
		this.properties.setHost("mongo.example.com");
		this.properties.setAdditionalHosts(Arrays.asList("mongo.example.com:33", "mongo.example2.com"));
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(3);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
		assertServerAddress(allAddresses.get(1), "mongo.example.com", 33);
		assertServerAddress(allAddresses.get(2), "mongo.example2.com", 27017);
	}

	@Test
	void credentialsCanBeCustomized() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertMongoCredential(settings.getCredential(), "user", "secret", "test");
	}

	@Test
	void replicaSetCanBeCustomized() {
		this.properties.setReplicaSetName("test");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getClusterSettings().getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void databaseCanBeCustomized() {
		this.properties.setDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertMongoCredential(settings.getCredential(), "user", "secret", "foo");
	}

	@Test
	void uuidRepresentationDefaultToJavaLegacy() {
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getUuidRepresentation()).isEqualTo(UuidRepresentation.JAVA_LEGACY);
	}

	@Test
	void uuidRepresentationCanBeCustomized() {
		this.properties.setUuidRepresentation(UuidRepresentation.STANDARD);
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getUuidRepresentation()).isEqualTo(UuidRepresentation.STANDARD);
	}

	@Test
	void authenticationDatabaseCanBeCustomized() {
		this.properties.setAuthenticationDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertMongoCredential(settings.getCredential(), "user", "secret", "foo");
	}

	@Test
	void onlyHostAndPortSetShouldUseThat() {
		this.properties.setHost("localhost");
		this.properties.setPort(27017);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void onlyUriSetShouldUseThat() {
		this.properties.setUri("mongodb://mongo1.example.com:12345");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void noCustomAddressAndNoUriUsesDefaultUri() {
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void uriCanBeCustomized() {
		this.properties.setUri("mongodb://user:secret@mongo1.example.com:12345,mongo2.example.com:23456/test");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		assertMongoCredential(settings.getCredential(), "user", "secret", "test");
	}

	@Test
	void uriOverridesUsernameAndPassword() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getCredential()).isNull();
	}

	@Test
	void uriOverridesDatabase() {
		this.properties.setUri("mongodb://secret:password@127.0.0.1:1234/mydb");
		this.properties.setDatabase("test");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "127.0.0.1", 1234);
		assertThat(settings.getCredential().getSource()).isEqualTo("mydb");
	}

	@Test
	void uriOverridesHostAndPort() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setHost("localhost");
		this.properties.setPort(4567);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> addresses = getAllAddresses(settings);
		assertThat(addresses.get(0).getHost()).isEqualTo("127.0.0.1");
		assertThat(addresses.get(0).getPort()).isEqualTo(1234);
	}

	@Test
	void retryWritesIsPropagatedFromUri() {
		this.properties.setUri("mongodb://localhost/test?retryWrites=false");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getRetryWrites()).isFalse();
	}

	@SuppressWarnings("removal")
	private MongoClientSettings customizeSettings() {
		MongoClientSettings.Builder settings = MongoClientSettings.builder();
		new MongoPropertiesClientSettingsBuilderCustomizer(this.properties).customize(settings);
		return settings.build();
	}

	private List<ServerAddress> getAllAddresses(MongoClientSettings settings) {
		return settings.getClusterSettings().getHosts();
	}

	protected void assertServerAddress(ServerAddress serverAddress, String expectedHost, int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	protected void assertMongoCredential(MongoCredential credentials, String expectedUsername, String expectedPassword,
			String expectedSource) {
		assertThat(credentials.getUserName()).isEqualTo(expectedUsername);
		assertThat(credentials.getPassword()).isEqualTo(expectedPassword.toCharArray());
		assertThat(credentials.getSource()).isEqualTo(expectedSource);
	}

}
