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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MongoClientFactorySupport}.
 *
 * @param <T> the mongo client type
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Scott Frederick
 */
abstract class MongoClientFactorySupportTests<T> {

	private final MongoProperties properties = new MongoProperties();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void canBindCharArrayPassword() {
		// gh-1572
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.password:word").applyTo(context);
		context.register(Config.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.getPassword()).isEqualTo("word".toCharArray());
	}

	@Test
	void allMongoClientSettingsCanBeSet() {
		MongoClientSettings.Builder builder = MongoClientSettings.builder();
		builder.applyToSocketSettings((settings) -> {
			settings.connectTimeout(1000, TimeUnit.MILLISECONDS);
			settings.readTimeout(1000, TimeUnit.MILLISECONDS);
		}).applyToServerSettings((settings) -> {
			settings.heartbeatFrequency(10001, TimeUnit.MILLISECONDS);
			settings.minHeartbeatFrequency(501, TimeUnit.MILLISECONDS);
		}).applyToConnectionPoolSettings((settings) -> {
			settings.maxWaitTime(120001, TimeUnit.MILLISECONDS);
			settings.maxConnectionLifeTime(60000, TimeUnit.MILLISECONDS);
			settings.maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS);
		}).applyToSslSettings((settings) -> settings.enabled(true)).applicationName("test");

		MongoClientSettings settings = builder.build();
		T client = createMongoClient(settings);
		MongoClientSettings wrapped = getClientSettings(client);
		assertThat(wrapped.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getServerSettings().getHeartbeatFrequency(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getServerSettings().getHeartbeatFrequency(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getServerSettings().getMinHeartbeatFrequency(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getServerSettings().getMinHeartbeatFrequency(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getApplicationName()).isEqualTo(settings.getApplicationName());
		assertThat(wrapped.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getSslSettings().isEnabled()).isEqualTo(settings.getSslSettings().isEnabled());
	}

	@Test
	void portCanBeCustomized() {
		this.properties.setPort(12345);
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	void hostCanBeCustomized() {
		this.properties.setHost("mongo.example.com");
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	void credentialsCanBeCustomized() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		T client = createMongoClient();
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "test");
	}

	@Test
	void replicaSetCanBeCustomized() {
		this.properties.setReplicaSetName("test");
		T client = createMongoClient();
		assertThat(getClientSettings(client).getClusterSettings().getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void databaseCanBeCustomized() {
		this.properties.setDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		T client = createMongoClient();
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "foo");
	}

	@Test
	void uuidRepresentationDefaultToJavaLegacy() {
		T client = createMongoClient();
		assertThat(getClientSettings(client).getUuidRepresentation()).isEqualTo(UuidRepresentation.JAVA_LEGACY);
	}

	@Test
	void uuidRepresentationCanBeCustomized() {
		this.properties.setUuidRepresentation(UuidRepresentation.STANDARD);
		T client = createMongoClient();
		assertThat(getClientSettings(client).getUuidRepresentation()).isEqualTo(UuidRepresentation.STANDARD);
	}

	@Test
	void authenticationDatabaseCanBeCustomized() {
		this.properties.setAuthenticationDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		T client = createMongoClient();
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "foo");
	}

	@Test
	void uriCanBeCustomized() {
		this.properties.setUri("mongodb://user:secret@mongo1.example.com:12345,mongo2.example.com:23456/test");
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		assertMongoCredential(getClientSettings(client).getCredential(), "user", "secret", "test");
	}

	@Test
	void uriIsIgnoredInEmbeddedMode() {
		this.properties.setUri("mongodb://mongo.example.com:1234/mydb");
		this.environment.setProperty("local.mongo.port", "4000");
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 4000);
	}

	@Test
	void retryWritesIsPropagatedFromUri() {
		this.properties.setUri("mongodb://localhost/test?retryWrites=true");
		T client = createMongoClient();
		assertThat(getClientSettings(client).getRetryWrites()).isTrue();
	}

	@Test
	void uriCannotBeSetWithCredentials() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		assertThatIllegalStateException().isThrownBy(this::createMongoClient).withMessageContaining(
				"Invalid mongo configuration, either uri or host/port/credentials/replicaSet must be specified");
	}

	@Test
	void uriCannotBeSetWithReplicaSetName() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setReplicaSetName("test");
		assertThatIllegalStateException().isThrownBy(this::createMongoClient).withMessageContaining(
				"Invalid mongo configuration, either uri or host/port/credentials/replicaSet must be specified");
	}

	@Test
	void uriCannotBeSetWithHostPort() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setHost("localhost");
		this.properties.setPort(4567);
		assertThatIllegalStateException().isThrownBy(this::createMongoClient).withMessageContaining(
				"Invalid mongo configuration, either uri or host/port/credentials/replicaSet must be specified");
	}

	@Test
	void customizerIsInvoked() {
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	@Test
	void customizerIsInvokedWhenHostIsSet() {
		this.properties.setHost("localhost");
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	@Test
	void customizerIsInvokedForEmbeddedMongo() {
		this.environment.setProperty("local.mongo.port", "27017");
		MongoClientSettingsBuilderCustomizer customizer = mock(MongoClientSettingsBuilderCustomizer.class);
		createMongoClient(customizer);
		verify(customizer).customize(any(MongoClientSettings.Builder.class));
	}

	@Test
	void onlyHostAndPortSetShouldUseThat() {
		this.properties.setHost("localhost");
		this.properties.setPort(27017);
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void onlyUriSetShouldUseThat() {
		this.properties.setUri("mongodb://mongo1.example.com:12345");
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void noCustomAddressAndNoUriUsesDefaultUri() {
		T client = createMongoClient();
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void canBindAutoIndexCreation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.mongodb.autoIndexCreation:true").applyTo(context);
		context.register(Config.class);
		context.refresh();
		MongoProperties properties = context.getBean(MongoProperties.class);
		assertThat(properties.isAutoIndexCreation()).isTrue();
	}

	private List<ServerAddress> getAllAddresses(T client) {
		return getClientSettings(client).getClusterSettings().getHosts();
	}

	protected T createMongoClient() {
		return createMongoClient(this.properties, this.environment, null, null);
	}

	protected T createMongoClient(MongoClientSettings settings) {
		return createMongoClient(this.properties, this.environment, null, settings);
	}

	protected void createMongoClient(MongoClientSettingsBuilderCustomizer... customizers) {
		createMongoClient(this.properties, this.environment, (customizers != null) ? Arrays.asList(customizers) : null,
				null);
	}

	protected abstract T createMongoClient(MongoProperties properties, Environment environment,
			List<MongoClientSettingsBuilderCustomizer> customizers, MongoClientSettings settings);

	protected abstract MongoClientSettings getClientSettings(T client);

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

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MongoProperties.class)
	static class Config {

	}

}
