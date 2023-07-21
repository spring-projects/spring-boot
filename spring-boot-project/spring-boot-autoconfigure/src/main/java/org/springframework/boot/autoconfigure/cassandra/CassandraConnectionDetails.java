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

package org.springframework.boot.autoconfigure.cassandra;

import java.util.List;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a Cassandra service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface CassandraConnectionDetails extends ConnectionDetails {

	/**
	 * Cluster node addresses.
	 * @return the cluster node addresses
	 */
	List<Node> getContactPoints();

	/**
	 * Login user of the server.
	 * @return the login user of the server or {@code null}
	 */
	default String getUsername() {
		return null;
	}

	/**
	 * Login password of the server.
	 * @return the login password of the server or {@code null}
	 */
	default String getPassword() {
		return null;
	}

	/**
	 * Datacenter that is considered "local". Contact points should be from this
	 * datacenter.
	 * @return the datacenter that is considered "local"
	 */
	String getLocalDatacenter();

	/**
	 * A Cassandra node.
	 *
	 * @param host the hostname
	 * @param port the port
	 */
	record Node(String host, int port) {
	}

}
