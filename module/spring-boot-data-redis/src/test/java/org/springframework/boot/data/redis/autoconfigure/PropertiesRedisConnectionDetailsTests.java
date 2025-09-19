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

package org.springframework.boot.data.redis.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails.Cluster;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails.MasterReplica;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails.Node;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails.Sentinel;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertiesDataRedisConnectionDetails}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PropertiesRedisConnectionDetailsTests {

	private DataRedisProperties properties;

	private PropertiesDataRedisConnectionDetails connectionDetails;

	private DefaultSslBundleRegistry sslBundleRegistry;

	@BeforeEach
	void setUp() {
		this.properties = new DataRedisProperties();
		this.sslBundleRegistry = new DefaultSslBundleRegistry();
		this.connectionDetails = new PropertiesDataRedisConnectionDetails(this.properties, this.sslBundleRegistry);
	}

	@Test
	void connectionIsConfiguredWithDefaults() {
		DataRedisConnectionDetails.Standalone standalone = this.connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("localhost");
		assertThat(standalone.getPort()).isEqualTo(6379);
		assertThat(standalone.getDatabase()).isEqualTo(0);
		assertThat(this.connectionDetails.getSentinel()).isNull();
		assertThat(this.connectionDetails.getCluster()).isNull();
		assertThat(this.connectionDetails.getMasterReplica()).isNull();
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
		DataRedisConnectionDetails.Standalone standalone = this.connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(9999);
	}

	@Test
	void standaloneIsConfiguredFromUrlWithoutDatabase() {
		this.properties.setUrl("redis://example.com:1234");
		this.properties.setDatabase(5);
		PropertiesDataRedisConnectionDetails connectionDetails = new PropertiesDataRedisConnectionDetails(
				this.properties, null);
		DataRedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(0);
	}

	@Test
	void standaloneIsConfiguredFromProperties() {
		this.properties.setHost("example.com");
		this.properties.setPort(1234);
		this.properties.setDatabase(5);
		DataRedisConnectionDetails.Standalone standalone = this.connectionDetails.getStandalone();
		assertThat(standalone.getHost()).isEqualTo("example.com");
		assertThat(standalone.getPort()).isEqualTo(1234);
		assertThat(standalone.getDatabase()).isEqualTo(5);
	}

	@Test
	void clusterIsConfigured() {
		DataRedisProperties.Cluster cluster = new DataRedisProperties.Cluster();
		cluster.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setCluster(cluster);
		Cluster actualCluster = this.connectionDetails.getCluster();
		assertThat(actualCluster).isNotNull();
		assertThat(actualCluster.getNodes()).containsExactly(new Node("localhost", 1111), new Node("127.0.0.1", 2222),
				new Node("[::1]", 3333));
	}

	@Test
	void masterReplicaIsConfigured() {
		DataRedisProperties.Masterreplica masterReplica = new DataRedisProperties.Masterreplica();
		masterReplica.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setMasterreplica(masterReplica);
		MasterReplica actualMasterReplica = this.connectionDetails.getMasterReplica();
		assertThat(actualMasterReplica).isNotNull();
		assertThat(actualMasterReplica.getNodes()).containsExactly(new Node("localhost", 1111),
				new Node("127.0.0.1", 2222), new Node("[::1]", 3333));
	}

	@Test
	void sentinelIsConfigured() {
		DataRedisProperties.Sentinel sentinel = new DataRedisProperties.Sentinel();
		sentinel.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setSentinel(sentinel);
		this.properties.setDatabase(5);
		PropertiesDataRedisConnectionDetails connectionDetails = new PropertiesDataRedisConnectionDetails(
				this.properties, null);
		Sentinel actualSentinel = connectionDetails.getSentinel();
		assertThat(actualSentinel).isNotNull();
		assertThat(actualSentinel.getNodes()).containsExactly(new Node("localhost", 1111), new Node("127.0.0.1", 2222),
				new Node("[::1]", 3333));
		assertThat(actualSentinel.getDatabase()).isEqualTo(5);
	}

	@Test
	void sentinelDatabaseIsConfiguredFromUrl() {
		DataRedisProperties.Sentinel sentinel = new DataRedisProperties.Sentinel();
		sentinel.setNodes(List.of("localhost:1111", "127.0.0.1:2222", "[::1]:3333"));
		this.properties.setSentinel(sentinel);
		this.properties.setUrl("redis://example.com:1234/9999");
		this.properties.setDatabase(5);
		PropertiesDataRedisConnectionDetails connectionDetails = new PropertiesDataRedisConnectionDetails(
				this.properties, null);
		Sentinel actualSentinel = connectionDetails.getSentinel();
		assertThat(actualSentinel).isNotNull();
		assertThat(actualSentinel.getDatabase()).isEqualTo(9999);
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
	void shouldReturnSystemBundleIfSslIsEnabledButBundleNotSet() {
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
