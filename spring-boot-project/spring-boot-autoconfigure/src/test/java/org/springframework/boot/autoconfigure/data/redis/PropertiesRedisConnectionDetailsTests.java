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

package org.springframework.boot.autoconfigure.data.redis;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesRedisConnectionDetails}.
 *
 * @author Scott Frederick
 */
class PropertiesRedisConnectionDetailsTests {

	private final RedisProperties properties = new RedisProperties();

	@Test
	void connectionIsConfiguredWithDefaults() {
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("localhost");
		assertThat(standalone.getPort()).isEqualTo(6379);
		assertThat(standalone.getDatabase()).isEqualTo(0);
		assertThat(connectionDetails.getSentinel()).isNull();
		assertThat(connectionDetails.getCluster()).isNull();
		assertThat(connectionDetails.getUsername()).isNull();
		assertThat(connectionDetails.getPassword()).isNull();
	}

	@Test
	void credentialsAreConfiguredFromUrlWithUsernameAndPassword() {
		this.properties.setUrl("redis://user:secret@example.com");
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		assertThat(connectionDetails.getUsername()).isEqualTo("user");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void credentialsAreConfiguredFromUrlWithUsernameAndColon() {
		this.properties.setUrl("redis://user:@example.com");
		this.properties.setUsername("notused");
		this.properties.setPassword("notused");
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		assertThat(connectionDetails.getUsername()).isEqualTo("user");
		assertThat(connectionDetails.getPassword()).isEmpty();
	}

	@Test
	void credentialsAreConfiguredFromUrlWithColonAndPassword() {
		this.properties.setUrl("redis://:secret@example.com");
		this.properties.setUsername("notused");
		this.properties.setPassword("notused");
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		assertThat(connectionDetails.getUsername()).isEmpty();
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void credentialsAreConfiguredFromUrlWithPasswordOnly() {
		this.properties.setUrl("redis://secret@example.com");
		this.properties.setUsername("notused");
		this.properties.setPassword("notused");
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		assertThat(connectionDetails.getUsername()).isNull();
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void credentialsAreConfiguredFromProperties() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret");
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		assertThat(connectionDetails.getUsername()).isEqualTo("user");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void standaloneIsConfiguredFromUrl() {
		this.properties.setUrl("redis://example.com:1234/9999");
		this.properties.setHost("notused");
		this.properties.setPort(9999);
		this.properties.setDatabase(5);
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(5);
	}

	@Test
	void standaloneIsConfiguredFromProperties() {
		this.properties.setHost("example.com");
		this.properties.setPort(1234);
		this.properties.setDatabase(5);
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(5);
	}

	@Test
	void clusterIsConfigured() {
		RedisProperties.Cluster cluster = new RedisProperties.Cluster();
		cluster.setNodes(List.of("first:1111", "second:2222", "third:3333"));
		this.properties.setCluster(cluster);
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties);
		assertThat(connectionDetails.getCluster().getNodes()).containsExactly(
				new RedisConnectionDetails.Node("first", 1111), new RedisConnectionDetails.Node("second", 2222),
				new RedisConnectionDetails.Node("third", 3333));
	}

}
