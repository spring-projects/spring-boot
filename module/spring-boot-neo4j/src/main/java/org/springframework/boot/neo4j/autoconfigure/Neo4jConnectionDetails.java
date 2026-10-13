/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.autoconfigure;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokenManager;
import org.neo4j.driver.AuthTokens;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a Neo4j service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface Neo4jConnectionDetails extends ConnectionDetails {

	/**
	 * Returns the URI of the Neo4j server. Defaults to {@code bolt://localhost:7687"}.
	 * @return the Neo4j server URI
	 */
	default URI getUri() {
		return URI.create("bolt://localhost:7687");
	}

	/**
	 * Returns the token to use for authentication. Defaults to {@link AuthTokens#none()}.
	 * @return the auth token
	 */
	default AuthToken getAuthToken() {
		return AuthTokens.none();
	}

	/**
	 * Returns the {@link AuthTokenManager} to use for authentication. Defaults to
	 * {@code null} in which case the {@link #getAuthToken() auth token} should be used.
	 * @return the auth token manager
	 */
	default @Nullable AuthTokenManager getAuthTokenManager() {
		return null;
	}

}
