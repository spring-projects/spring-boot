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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.lang.invoke.MethodHandles;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;

/**
 * A class that represents credentials for a server.
 *
 * @author Dmytro Nosan
 */
class Credentials extends MappedObject {

	/**
	 * If the secret being stored is an identity token, the username should be set to
	 * {@code <token>}.
	 */
	private static final String TOKEN_USERNAME = "<token>";

	private final String serverUrl;

	private final String username;

	private final String secret;

	/**
	 * Create a new {@link Credentials} instance from the given JSON node.
	 * @param node the JSON node to read from
	 */
	Credentials(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.serverUrl = valueAt("/ServerURL", String.class);
		this.username = valueAt("/Username", String.class);
		this.secret = valueAt("/Secret", String.class);
	}

	/**
	 * Checks if the secret being stored is an identity token.
	 * @return true if the secret is an identity token, false otherwise
	 */
	boolean isIdentityToken() {
		return TOKEN_USERNAME.equals(this.username);
	}

	/**
	 * Returns the server URL associated with this credential.
	 * @return the server URL
	 */
	String getServerUrl() {
		return this.serverUrl;
	}

	/**
	 * Returns the username associated with the credential.
	 * @return the username
	 */
	String getUsername() {
		return this.username;
	}

	/**
	 * Returns the secret associated with this credential.
	 * @return the secret
	 */
	String getSecret() {
		return this.secret;
	}

}
