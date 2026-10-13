/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a Couchbase service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface CouchbaseConnectionDetails extends ConnectionDetails {

	/**
	 * Connection string used to locate the Couchbase cluster.
	 * @return the connection string used to locate the Couchbase cluster
	 */
	String getConnectionString();

	/**
	 * Cluster username.
	 * @return the cluster username
	 */
	@Nullable String getUsername();

	/**
	 * Cluster password.
	 * @return the cluster password
	 */
	@Nullable String getPassword();

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

}
