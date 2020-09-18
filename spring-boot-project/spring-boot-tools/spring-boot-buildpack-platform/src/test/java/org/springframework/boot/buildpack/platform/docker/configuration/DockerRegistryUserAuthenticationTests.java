/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;

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
		JSONAssert.assertEquals(jsonContent("auth-user-full.json"), decoded(auth.getAuthHeader()), false);
	}

	@Test
	void createFullAuthHeaderReturnsEncodedHeader() throws IOException, JSONException {
		DockerRegistryUserAuthentication auth = new DockerRegistryUserAuthentication("user", "secret", null, null);
		JSONAssert.assertEquals(jsonContent("auth-user-minimal.json"), decoded(auth.getAuthHeader()), false);
	}

	private String jsonContent(String s) throws IOException {
		return StreamUtils.copyToString(getContent(s), StandardCharsets.UTF_8);
	}

	private String decoded(String header) {
		return new String(Base64Utils.decodeFromUrlSafeString(header));
	}

}
