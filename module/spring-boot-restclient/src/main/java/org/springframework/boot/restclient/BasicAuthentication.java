/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

import java.nio.charset.Charset;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Basic authentication details to be applied to {@link HttpHeaders}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 */
class BasicAuthentication {

	private final String username;

	private final String password;

	private final @Nullable Charset charset;

	BasicAuthentication(String username, String password, @Nullable Charset charset) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");
		this.username = username;
		this.password = password;
		this.charset = charset;
	}

	void applyTo(HttpHeaders headers) {
		if (!headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
			headers.setBasicAuth(this.username, this.password, this.charset);
		}
	}

}
