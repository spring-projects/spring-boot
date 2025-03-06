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

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * Details required to establish a connection to a Redis service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface RedisConnectionDetails extends ConnectionDetails {

	/**
	 * Login username of the redis server.
	 * @return the login username of the redis server
	 */
	default String getUsername() {
		return null;
	}

	/**
	 * Login password of the redis server.
	 * @return the login password of the redis server
	 */
	default String getPassword() {
		return null;
	}

	/**
	 * Redis standalone configuration. Mutually exclusive with {@link #getSentinel()} and
	 * {@link #getCluster()}.
	 * @return the Redis standalone configuration
	 */
	default Standalone getStandalone() {
		return null;
	}

	/**
	 * Redis sentinel configuration. Mutually exclusive with {@link #getStandalone()} and
	 * {@link #getCluster()}.
	 * @return the Redis sentinel configuration
	 */
	default Sentinel getSentinel() {
		return null;
	}

	/**
	 * Redis cluster configuration. Mutually exclusive with {@link #getStandalone()} and
	 * {@link #getSentinel()}.
	 * @return the Redis cluster configuration
	 */
	default Cluster getCluster() {
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
		 * SSL bundle to use.
		 * @return the SSL bundle to use
		 * @since 3.5.0
		 */
		default SslBundle getSslBundle() {
			return null;
		}

		/**
		 * Creates a new instance with the given host and port.
		 * @param host the host
		 * @param port the port
		 * @return the new instance
		 */
		static Standalone of(String host, int port) {
			return of(host, port, 0, null);
		}

		/**
		 * Creates a new instance with the given host, port and SSL bundle.
		 * @param host the host
		 * @param port the port
		 * @param sslBundle the SSL bundle
		 * @return the new instance
		 * @since 3.5.0
		 */
		static Standalone of(String host, int port, SslBundle sslBundle) {
			return of(host, port, 0, sslBundle);
		}

		/**
		 * Creates a new instance with the given host, port and database.
		 * @param host the host
		 * @param port the port
		 * @param database the database
		 * @return the new instance
		 */
		static Standalone of(String host, int port, int database) {
			return of(host, port, database, null);
		}

		/**
		 * Creates a new instance with the given host, port, database and SSL bundle.
		 * @param host the host
		 * @param port the port
		 * @param database the database
		 * @param sslBundle the SSL bundle
		 * @return the new instance
		 * @since 3.5.0
		 */
		static Standalone of(String host, int port, int database, SslBundle sslBundle) {
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

				@Override
				public SslBundle getSslBundle() {
					return sslBundle;
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
		String getUsername();

		/**
		 * Password for authenticating with sentinel(s).
		 * @return the password for authenticating with sentinel(s) or {@code null}
		 */
		String getPassword();

		/**
		 * SSL bundle to use.
		 * @return the SSL bundle to use
		 * @since 3.5.0
		 */
		default SslBundle getSslBundle() {
			return null;
		}

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

		/**
		 * SSL bundle to use.
		 * @return the SSL bundle to use
		 * @since 3.5.0
		 */
		default SslBundle getSslBundle() {
			return null;
		}

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
