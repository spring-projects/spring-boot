/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerRegistryUserAuthentication}.
 *
 * @author Scott Frederick
 */
class DockerRegistryUserAuthenticationTests extends AbstractJsonTests {

	@Test
	void createMinimalAuthHeaderReturnsEncodedHeader() throws IOException, JSONException {
		DockerRegistryUserAuthentication auth = new DockerRegistryUserAuthentication("user", "secret",
				"https://docker.example.com", "docker@example.com");
		String authHeader = auth.getAuthHeader();
		assertThat(authHeader).isNotNull();
		JSONAssert.assertEquals(jsonContent("auth-user-full.json"), decoded(authHeader), true);
	}

	@Test
	void createFullAuthHeaderReturnsEncodedHeader() throws IOException, JSONException {
		DockerRegistryUserAuthentication auth = new DockerRegistryUserAuthentication("user", "secret", null, null);
		String authHeader = auth.getAuthHeader();
		assertThat(authHeader).isNotNull();
		JSONAssert.assertEquals(jsonContent("auth-user-minimal.json"), decoded(authHeader), false);
	}

	private String jsonContent(String s) throws IOException {
		return StreamUtils.copyToString(getContent(s), StandardCharsets.UTF_8);
	}

	private String decoded(String header) {
		return new String(Base64.getUrlDecoder().decode(header));
	}

}
