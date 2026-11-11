/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.lang.invoke.MethodHandles;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.util.Assert;

/**
 * A class that represents credentials for a server as returned from a
 * {@link CredentialHelper}.
 *
 * @author Dmytro Nosan
 */
class Credential extends MappedObject {

	/**
	 * If the secret being stored is an identity token, the username should be set to
	 * {@code <token>}.
	 */
	private static final String TOKEN_USERNAME = "<token>";

	private final String username;

	private final String secret;

	private final @Nullable String serverUrl;

	Credential(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.username = extractUsername();
		this.secret = extractSecret();
		this.serverUrl = valueAt("/ServerURL", String.class);
	}

	private String extractSecret() {
		String result = valueAt("/Secret", String.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	private String extractUsername() {
		String result = valueAt("/Username", String.class);
		Assert.state(result != null, "'result' must not be null");
		return result;
	}

	String getUsername() {
		return this.username;
	}

	String getSecret() {
		return this.secret;
	}

	@Nullable String getServerUrl() {
		return this.serverUrl;
	}

	boolean isIdentityToken() {
		return TOKEN_USERNAME.equals(this.username);
	}

}
