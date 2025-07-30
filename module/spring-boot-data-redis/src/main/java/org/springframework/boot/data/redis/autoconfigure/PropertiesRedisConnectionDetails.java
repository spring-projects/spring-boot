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

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link RedisProperties} to {@link RedisConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Yanming Zhou
 * @author Phillip Webb
 */
class PropertiesRedisConnectionDetails implements RedisConnectionDetails {

	private final RedisProperties properties;

	private final @Nullable SslBundles sslBundles;

	PropertiesRedisConnectionDetails(RedisProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public @Nullable String getUsername() {
		RedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null) ? redisUrl.credentials().username() : this.properties.getUsername();
	}

	@Override
	public @Nullable String getPassword() {
		RedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null) ? redisUrl.credentials().password() : this.properties.getPassword();
	}

	@Override
	public Standalone getStandalone() {
		RedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null)
				? Standalone.of(redisUrl.uri().getHost(), redisUrl.uri().getPort(), redisUrl.database(), getSslBundle())
				: Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase(),
						getSslBundle());
	}

	private @Nullable SslBundle getSslBundle() {
		if (!this.properties.getSsl().isEnabled()) {
			return null;
		}
		String bundleName = this.properties.getSsl().getBundle();
		if (StringUtils.hasLength(bundleName)) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(bundleName);
		}
		return SslBundle.systemDefault();
	}

	@Override
	public @Nullable Sentinel getSentinel() {
		RedisProperties.Sentinel sentinel = this.properties.getSentinel();
		return (sentinel != null) ? new PropertiesSentinel(getStandalone().getDatabase(), sentinel) : null;
	}

	@Override
	public @Nullable Cluster getCluster() {
		RedisProperties.Cluster cluster = this.properties.getCluster();
		return (cluster != null) ? new PropertiesCluster(cluster) : null;
	}

	private @Nullable RedisUrl getRedisUrl() {
		return RedisUrl.of(this.properties.getUrl());
	}

	private List<Node> asNodes(@Nullable List<String> nodes) {
		if (nodes == null) {
			return Collections.emptyList();
		}
		return nodes.stream().map(this::asNode).toList();
	}

	private Node asNode(String node) {
		int portSeparatorIndex = node.lastIndexOf(':');
		String host = node.substring(0, portSeparatorIndex);
		int port = Integer.parseInt(node.substring(portSeparatorIndex + 1));
		return new Node(host, port);
	}

	/**
	 * {@link Cluster} implementation backed by properties.
	 */
	private class PropertiesCluster implements Cluster {

		private final List<Node> nodes;

		PropertiesCluster(RedisProperties.Cluster properties) {
			this.nodes = asNodes(properties.getNodes());
		}

		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return PropertiesRedisConnectionDetails.this.getSslBundle();
		}

	}

	/**
	 * {@link Sentinel} implementation backed by properties.
	 */
	private class PropertiesSentinel implements Sentinel {

		private final int database;

		private final RedisProperties.Sentinel properties;

		PropertiesSentinel(int database, RedisProperties.Sentinel properties) {
			this.database = database;
			this.properties = properties;
		}

		@Override
		public int getDatabase() {
			return this.database;
		}

		@Override
		public String getMaster() {
			String master = this.properties.getMaster();
			Assert.state(master != null, "'master' must not be null");
			return master;
		}

		@Override
		public List<Node> getNodes() {
			return asNodes(this.properties.getNodes());
		}

		@Override
		public @Nullable String getUsername() {
			return this.properties.getUsername();
		}

		@Override
		public @Nullable String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return PropertiesRedisConnectionDetails.this.getSslBundle();
		}

	}

}
