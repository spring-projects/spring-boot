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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 * @author Artsiom Yudovin
 */
class MongoPropertiesTests {

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
	@SuppressWarnings("deprecation")
	void allMongoClientOptionsCanBeSet() {
		MongoClientSettings.Builder builder = MongoClientSettings.builder();
		builder.applyToSocketSettings((settings) -> {
			settings.connectTimeout(1000, TimeUnit.MILLISECONDS);
			settings.readTimeout(1000, TimeUnit.MILLISECONDS);
		}).applyToServerSettings((settings) -> {
			settings.heartbeatFrequency(10001, TimeUnit.MILLISECONDS);
			settings.minHeartbeatFrequency(501, TimeUnit.MILLISECONDS);
		}).applyToClusterSettings((settings) -> settings.requiredReplicaSetName("testReplicaSetName"))
				.applyToConnectionPoolSettings((settings) -> {
					settings.maxWaitTime(120001, TimeUnit.MILLISECONDS);
					settings.maxConnectionLifeTime(60000, TimeUnit.MILLISECONDS);
					settings.maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS);
				}).applyToSslSettings((settings) -> settings.enabled(true)).applicationName("test");

		MongoClientSettings settings = builder.build();
		MongoProperties properties = new MongoProperties();
		MongoClient client = new MongoClientFactory(properties, null, Collections.emptyList())
				.createMongoClient(settings);
		MongoClientSettings wrapped = (MongoClientSettings) ReflectionTestUtils.getField(client, "settings");
		assertThat(wrapped.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getServerSettings().getHeartbeatFrequency(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getServerSettings().getHeartbeatFrequency(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getServerSettings().getMinHeartbeatFrequency(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getServerSettings().getMinHeartbeatFrequency(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getApplicationName()).isEqualTo(settings.getApplicationName());
		assertThat(wrapped.getClusterSettings().getRequiredReplicaSetName())
				.isEqualTo(settings.getClusterSettings().getRequiredReplicaSetName());
		assertThat(wrapped.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS))
				.isEqualTo(settings.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS));
		assertThat(wrapped.getSslSettings().isEnabled()).isEqualTo(settings.getSslSettings().isEnabled());
	}

	@Test
	void uriOverridesHostAndPort() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("localhost");
		properties.setPort(27017);
		properties.setUri("mongodb://mongo1.example.com:12345");
		MongoClient client = new MongoClientFactory(properties, null, Collections.emptyList()).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void onlyHostAndPortSetShouldUseThat() {
		MongoProperties properties = new MongoProperties();
		properties.setHost("localhost");
		properties.setPort(27017);
		MongoClient client = new MongoClientFactory(properties, null, Collections.emptyList()).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void onlyUriSetShouldUseThat() {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://mongo1.example.com:12345");
		MongoClient client = new MongoClientFactory(properties, null, Collections.emptyList()).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void noCustomAddressAndNoUriUsesDefaultUri() {
		MongoProperties properties = new MongoProperties();
		MongoClient client = new MongoClientFactory(properties, null, Collections.emptyList()).createMongoClient(null);
		List<ServerAddress> allAddresses = getAllAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "127.0.0.1", 27017);
	}

	@SuppressWarnings("deprecation")
	private List<ServerAddress> getAllAddresses(MongoClient client) {
		// At some point we'll probably need to use reflection to find the address but for
		// now, we can use the deprecated getAllAddress method.
		return client.getClusterDescription().getClusterSettings().getHosts();
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

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost, int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MongoProperties.class)
	static class Config {

	}

}
