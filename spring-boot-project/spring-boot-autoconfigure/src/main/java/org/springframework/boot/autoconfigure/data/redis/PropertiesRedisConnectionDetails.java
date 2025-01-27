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

	PropertiesRedisConnectionDetails(RedisProperties properties) {
		this.properties = properties;
	}

	@Override
	public String getUsername() {
		RedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null) ? redisUrl.credentials().username() : this.properties.getUsername();
	}

	@Override
	public String getPassword() {
		RedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null) ? redisUrl.credentials().password() : this.properties.getPassword();
	}

	@Override
	public Standalone getStandalone() {
		RedisUrl redisUrl = getRedisUrl();
		return (redisUrl != null)
				? Standalone.of(redisUrl.uri().getHost(), redisUrl.uri().getPort(), redisUrl.database())
				: Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase());
	}

	@Override
	public Sentinel getSentinel() {
		RedisProperties.Sentinel sentinel = this.properties.getSentinel();
		return (sentinel != null) ? new PropertiesSentinel(getStandalone().getDatabase(), sentinel) : null;
	}

	@Override
	public Cluster getCluster() {
		RedisProperties.Cluster cluster = this.properties.getCluster();
		List<Node> nodes = (cluster != null) ? asNodes(cluster.getNodes()) : null;
		return (nodes != null) ? () -> nodes : null;
	}

	private RedisUrl getRedisUrl() {
		return RedisUrl.of(this.properties.getUrl());
	}

	private List<Node> asNodes(List<String> nodes) {
		return nodes.stream().map(this::asNode).toList();
	}

	private Node asNode(String node) {
		int portSeparatorIndex = node.lastIndexOf(':');
		String host = node.substring(0, portSeparatorIndex);
		int port = Integer.parseInt(node.substring(portSeparatorIndex + 1));
		return new Node(host, port);
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
			return this.properties.getMaster();
		}

		@Override
		public List<Node> getNodes() {
			return asNodes(this.properties.getNodes());
		}

		@Override
		public String getUsername() {
			return this.properties.getUsername();
		}

		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

	}

}
