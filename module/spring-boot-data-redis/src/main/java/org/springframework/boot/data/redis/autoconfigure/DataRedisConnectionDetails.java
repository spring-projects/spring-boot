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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * Details required to establish a connection to a Redis service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface DataRedisConnectionDetails extends ConnectionDetails {

	/**
	 * Login username of the redis server.
	 * @return the login username of the redis server
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Login password of the redis server.
	 * @return the login password of the redis server
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

	/**
	 * Redis standalone configuration. Mutually exclusive with {@link #getSentinel()},
	 * {@link #getCluster()} and {@link #getMasterReplica()}.
	 * @return the Redis standalone configuration
	 */
	default @Nullable Standalone getStandalone() {
		return null;
	}

	/**
	 * Redis sentinel configuration. Mutually exclusive with {@link #getStandalone()},
	 * {@link #getCluster()} and {@link #getMasterReplica()}.
	 * @return the Redis sentinel configuration
	 */
	default @Nullable Sentinel getSentinel() {
		return null;
	}

	/**
	 * Redis cluster configuration. Mutually exclusive with {@link #getStandalone()},
	 * {@link #getSentinel()} and {@link #getMasterReplica()}.
	 * @return the Redis cluster configuration
	 */
	default @Nullable Cluster getCluster() {
		return null;
	}

	/**
	 * Redis master replica configuration. Mutually exclusive with
	 * {@link #getStandalone()}, {@link #getSentinel()} and {@link #getCluster()}.
	 * @return the Redis master replica configuration
	 */
	default @Nullable MasterReplica getMasterReplica() {
		return null;
	}

	/**
	 * Redis standalone configuration.
	 */
	interface Standalone {

		/**
		 * Redis server host.
		 * @return the redis server host
		 */
		String getHost();

		/**
		 * Redis server port.
		 * @return the redis server port
		 */
		int getPort();

		/**
		 * Database index used by the connection factory.
		 * @return the database index used by the connection factory
		 */
		default int getDatabase() {
			return 0;
		}

		/**
		 * Creates a new instance with the given host and port.
		 * @param host the host
		 * @param port the port
		 * @return the new instance
		 */
		static Standalone of(String host, int port) {
			return of(host, port, 0);
		}

		/**
		 * Creates a new instance with the given host, port and database.
		 * @param host the host
		 * @param port the port
		 * @param database the database
		 * @return the new instance
		 */
		static Standalone of(String host, int port, int database) {
			Assert.hasLength(host, "'host' must not be empty");
			return new Standalone() {

				@Override
				public String getHost() {
					return host;
				}

				@Override
				public int getPort() {
					return port;
				}

				@Override
				public int getDatabase() {
					return database;
				}

			};
		}

	}

	/**
	 * Redis sentinel configuration.
	 */
	interface Sentinel {

		/**
		 * Database index used by the connection factory.
		 * @return the database index used by the connection factory
		 */
		int getDatabase();

		/**
		 * Name of the Redis server.
		 * @return the name of the Redis server
		 */
		String getMaster();

		/**
		 * List of nodes.
		 * @return the list of nodes
		 */
		List<Node> getNodes();

		/**
		 * Login username for authenticating with sentinel(s).
		 * @return the login username for authenticating with sentinel(s) or {@code null}
		 */
		@Nullable String getUsername();

		/**
		 * Password for authenticating with sentinel(s).
		 * @return the password for authenticating with sentinel(s) or {@code null}
		 */
		@Nullable String getPassword();

	}

	/**
	 * Redis cluster configuration.
	 */
	interface Cluster {

		/**
		 * Nodes to bootstrap from. This represents an "initial" list of cluster nodes and
		 * is required to have at least one entry.
		 * @return nodes to bootstrap from
		 */
		List<Node> getNodes();

	}

	/**
	 * Redis master replica configuration.
	 */
	interface MasterReplica {

		/**
		 * Static nodes to use. This represents the full list of cluster nodes and is
		 * required to have at least one entry.
		 * @return the nodes to use
		 */
		List<Node> getNodes();

	}

	/**
	 * A node in a sentinel or cluster configuration.
	 *
	 * @param host the hostname of the node
	 * @param port the port of the node
	 */
	record Node(String host, int port) {

	}

}
