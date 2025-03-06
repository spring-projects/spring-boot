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

package org.springframework.boot.autoconfigure.data.redis;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Node;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertiesRedisConnectionDetails}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PropertiesRedisConnectionDetailsTests {

	private RedisProperties properties;

	private PropertiesRedisConnectionDetails connectionDetails;

	private DefaultSslBundleRegistry sslBundleRegistry;

	@BeforeEach
	void setUp() {
		this.properties = new RedisProperties();
		this.sslBundleRegistry = new DefaultSslBundleRegistry();
		this.connectionDetails = new PropertiesRedisConnectionDetails(this.properties, this.sslBundleRegistry);
	}

	@Test
	void connectionIsConfiguredWithDefaults() {
		RedisConnectionDetails.Standalone standalone = this.connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("localhost");
		assertThat(standalone.getPort()).isEqualTo(6379);
		assertThat(standalone.getDatabase()).isEqualTo(0);
		assertThat(this.connectionDetails.getSentinel()).isNull();
		assertThat(this.connectionDetails.getCluster()).isNull();
		assertThat(this.connectionDetails.getUsername()).isNull();
		assertThat(this.connectionDetails.getPassword()).isNull();
	}

	@Test
	void credentialsAreConfiguredFromUrlWithUsernameAndPassword() {
		this.properties.setUrl("redis://user:secret@example.com");
		assertThat(this.connectionDetails.getUsername()).isEqualTo("user");
		assertThat(this.connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void credentialsAreConfiguredFromUrlWithUsernameAndColon() {
		this.properties.setUrl("redis://user:@example.com");
		this.properties.setUsername("notused");
		this.properties.setPassword("notused");
		assertThat(this.connectionDetails.getUsername()).isEqualTo("user");
		assertThat(this.connectionDetails.getPassword()).isEmpty();
	}

	@Test
	void credentialsAreConfiguredFromUrlWithColonAndPassword() {
		this.properties.setUrl("redis://:secret@example.com");
		this.properties.setUsername("notused");
		this.properties.setPassword("notused");
		assertThat(this.connectionDetails.getUsername()).isEmpty();
		assertThat(this.connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void credentialsAreConfiguredFromUrlWithPasswordOnly() {
		this.properties.setUrl("redis://secret@example.com");
		this.properties.setUsername("notused");
		this.properties.setPassword("notused");
		assertThat(this.connectionDetails.getUsername()).isNull();
		assertThat(this.connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void credentialsAreConfiguredFromProperties() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret");
		assertThat(this.connectionDetails.getUsername()).isEqualTo("user");
		assertThat(this.connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void standaloneIsConfiguredFromUrl() {
		this.properties.setUrl("redis://example.com:1234/9999");
		this.properties.setHost("notused");
		this.properties.setPort(9999);
		this.properties.setDatabase(5);
		RedisConnectionDetails.Standalone standalone = this.connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(9999);
	}

	@Test
	void standaloneIsConfiguredFromUrlWithoutDatabase() {
		this.properties.setUrl("redis://example.com:1234");
		this.properties.setDatabase(5);
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties,
				null);
		RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(0);
	}

	@Test
	void standaloneIsConfiguredFromProperties() {
		this.properties.setHost("example.com");
		this.properties.setPort(1234);
		this.properties.setDatabase(5);
		RedisConnectionDetails.Standalone standalone = this.connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(5);
	}

	@Test
	void clusterIsConfigured() {
		RedisProperties.Cluster cluster = new RedisProperties.Cluster();
		cluster.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setCluster(cluster);
		assertThat(this.connectionDetails.getCluster().getNodes()).containsExactly(new Node("localhost", 1111),
				new Node("127.0.0.1", 2222), new Node("[::1]", 3333));
	}

	@Test
	void sentinelIsConfigured() {
		RedisProperties.Sentinel sentinel = new RedisProperties.Sentinel();
		sentinel.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setSentinel(sentinel);
		this.properties.setDatabase(5);
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties,
				null);
		assertThat(connectionDetails.getSentinel().getNodes()).containsExactly(new Node("localhost", 1111),
				new Node("127.0.0.1", 2222), new Node("[::1]", 3333));
		assertThat(connectionDetails.getSentinel().getDatabase()).isEqualTo(5);
	}

	@Test
	void sentinelDatabaseIsConfiguredFromUrl() {
		RedisProperties.Sentinel sentinel = new RedisProperties.Sentinel();
		sentinel.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setSentinel(sentinel);
		this.properties.setUrl("redis://example.com:1234/9999");
		this.properties.setDatabase(5);
		PropertiesRedisConnectionDetails connectionDetails = new PropertiesRedisConnectionDetails(this.properties,
				null);
		assertThat(connectionDetails.getSentinel().getDatabase()).isEqualTo(9999);
	}

	@Test
	void shouldReturnSslBundle() {
		SslBundle bundle1 = mock(SslBundle.class);
		this.sslBundleRegistry.registerBundle("bundle-1", bundle1);
		this.properties.getSsl().setBundle("bundle-1");
		SslBundle sslBundle = this.connectionDetails.getStandalone().getSslBundle();
		assertThat(sslBundle).isSameAs(bundle1);
	}

	@Test
	void shouldReturnSystemBundleIfSslIsEnabledButBundleNotSet() {
		this.properties.getSsl().setEnabled(true);
		SslBundle sslBundle = this.connectionDetails.getStandalone().getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	@Test
	void shouldReturnNullIfSslIsNotEnabled() {
		this.properties.getSsl().setEnabled(false);
		SslBundle sslBundle = this.connectionDetails.getStandalone().getSslBundle();
		assertThat(sslBundle).isNull();
	}

}
