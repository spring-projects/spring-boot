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

package org.springframework.boot.autoconfigure.neo4j;

import java.net.URI;

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
 * @since 3.1.0
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
	 * @since 3.2.0
	 */
	default AuthTokenManager getAuthTokenManager() {
		return null;
	}

}
