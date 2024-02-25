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

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionConfiguration.ConnectionInfo;

/**
 * Adapts {@link RedisProperties} to {@link RedisConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PropertiesRedisConnectionDetails implements RedisConnectionDetails {

	private final RedisProperties properties;

	/**
	 * Constructs a new instance of PropertiesRedisConnectionDetails with the specified
	 * RedisProperties.
	 * @param properties the RedisProperties to be used for the connection details
	 */
	PropertiesRedisConnectionDetails(RedisProperties properties) {
		this.properties = properties;
	}

	/**
	 * Returns the username for the Redis connection. If the URL property is not null, it
	 * retrieves the username from the connection information. Otherwise, it returns the
	 * username from the properties.
	 * @return the username for the Redis connection
	 */
	@Override
	public String getUsername() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return connectionInfo.getUsername();
		}
		return this.properties.getUsername();
	}

	/**
	 * Retrieves the password for the Redis connection. If the URL property is not null,
	 * it retrieves the password from the connection info. Otherwise, it retrieves the
	 * password from the properties.
	 * @return the password for the Redis connection
	 */
	@Override
	public String getPassword() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return connectionInfo.getPassword();
		}
		return this.properties.getPassword();
	}

	/**
	 * Retrieves the Standalone instance based on the connection details provided in the
	 * PropertiesRedisConnectionDetails class. If the URL property is not null, it creates
	 * a ConnectionInfo object using the URL and returns a Standalone instance with the
	 * host, port, and database extracted from the ConnectionInfo object. If the URL
	 * property is null, it returns a Standalone instance with the host, port, and
	 * database provided in the PropertiesRedisConnectionDetails class.
	 * @return The Standalone instance representing the Redis connection details.
	 */
	@Override
	public Standalone getStandalone() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return Standalone.of(connectionInfo.getUri().getHost(), connectionInfo.getUri().getPort(),
					this.properties.getDatabase());
		}
		return Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase());
	}

	/**
	 * Retrieves the connection information for the given URL.
	 * @param url the URL to parse and retrieve the connection information from
	 * @return the ConnectionInfo object containing the parsed connection information, or
	 * null if the URL is null
	 */
	private ConnectionInfo connectionInfo(String url) {
		return (url != null) ? RedisConnectionConfiguration.parseUrl(url) : null;
	}

	/**
	 * Retrieves the Sentinel configuration details for the Redis connection.
	 * @return The Sentinel configuration details, or null if not configured.
	 */
	@Override
	public Sentinel getSentinel() {
		org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel sentinel = this.properties
			.getSentinel();
		if (sentinel == null) {
			return null;
		}
		return new Sentinel() {

			@Override
			public int getDatabase() {
				return PropertiesRedisConnectionDetails.this.properties.getDatabase();
			}

			@Override
			public String getMaster() {
				return sentinel.getMaster();
			}

			@Override
			public List<Node> getNodes() {
				return sentinel.getNodes().stream().map(PropertiesRedisConnectionDetails.this::asNode).toList();
			}

			@Override
			public String getUsername() {
				return sentinel.getUsername();
			}

			@Override
			public String getPassword() {
				return sentinel.getPassword();
			}

		};
	}

	/**
	 * Returns the Redis cluster associated with this connection details.
	 * @return the Redis cluster, or null if not available
	 */
	@Override
	public Cluster getCluster() {
		RedisProperties.Cluster cluster = this.properties.getCluster();
		List<Node> nodes = (cluster != null) ? cluster.getNodes().stream().map(this::asNode).toList() : null;
		return (nodes != null) ? () -> nodes : null;
	}

	/**
	 * Converts a string representation of a node into a Node object.
	 * @param node the string representation of the node in the format "host:port"
	 * @return the Node object representing the specified node
	 * @throws NumberFormatException if the port number cannot be parsed as an integer
	 */
	private Node asNode(String node) {
		String[] components = node.split(":");
		return new Node(components[0], Integer.parseInt(components[1]));
	}

}
