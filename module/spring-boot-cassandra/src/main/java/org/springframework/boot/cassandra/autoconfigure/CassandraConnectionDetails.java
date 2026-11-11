/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a Cassandra service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
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
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Login password of the server.
	 * @return the login password of the server or {@code null}
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * Datacenter that is considered "local". Contact points should be from this
	 * datacenter.
	 * @return the datacenter that is considered "local"
	 */
	@Nullable String getLocalDatacenter();

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

	/**
	 * A Cassandra node.
	 *
	 * @param host the hostname
	 * @param port the port
	 */
	record Node(String host, int port) {
	}

}
