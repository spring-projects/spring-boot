/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Cloud Foundry security service to handle REST calls to the cloud controller and UAA.
 *
 * @author Madhura Bhave
 */
class CloudFoundrySecurityService {

	private final RestTemplate restTemplate;

	private final String cloudControllerUrl;

	private String uaaUrl;

	CloudFoundrySecurityService(RestTemplateBuilder restTemplateBuilder,
			String cloudControllerUrl, boolean skipSslValidation) {
		Assert.notNull(restTemplateBuilder, "RestTemplateBuilder must not be null");
		Assert.notNull(cloudControllerUrl, "CloudControllerUrl must not be null");
		if (skipSslValidation) {
			restTemplateBuilder = restTemplateBuilder
					.requestFactory(SkipSslVerificationHttpRequestFactory.class);
		}
		this.restTemplate = restTemplateBuilder.build();
		this.cloudControllerUrl = cloudControllerUrl;
	}

	/**
	 * Return the access level that should be granted to the given token.
	 * @param token the token
	 * @param applicationId the cloud foundry application ID
	 * @return the access level that should be granted
	 * @throws CloudFoundryAuthorizationException if the token is not authorized
	 */
	public AccessLevel getAccessLevel(String token, String applicationId)
			throws CloudFoundryAuthorizationException {
		try {
			URI uri = getPermissionsUri(applicationId);
			RequestEntity<?> request = RequestEntity.get(uri)
					.header("Authorization", "bearer " + token).build();
			Map<?, ?> body = this.restTemplate.exchange(request, Map.class).getBody();
			if (Boolean.TRUE.equals(body.get("read_sensitive_data"))) {
				return AccessLevel.FULL;
			}
			return AccessLevel.RESTRICTED;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
				throw new CloudFoundryAuthorizationException(Reason.ACCESS_DENIED,
						"Access denied");
			}
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN,
					"Invalid token", ex);
		}
		catch (HttpServerErrorException ex) {
			throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"Cloud controller not reachable");
		}
	}

	private URI getPermissionsUri(String applicationId) {
		try {
			return new URI(this.cloudControllerUrl + "/v2/apps/" + applicationId
					+ "/permissions");
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Return all token keys known by the UAA.
	 * @return a list of token keys
	 */
	public Map<String, String> fetchTokenKeys() {
		try {
			return extractTokenKeys(this.restTemplate
					.getForObject(getUaaUrl() + "/token_keys", Map.class));
		}
		catch (HttpStatusCodeException e) {
			throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"UAA not reachable");
		}
	}

	private Map<String, String> extractTokenKeys(Map<?, ?> response) {
		Map<String, String> tokenKeys = new HashMap<>();
		for (Object key : (List<?>) response.get("keys")) {
			Map<?, ?> tokenKey = (Map<?, ?>) key;
			tokenKeys.put((String) tokenKey.get("kid"), (String) tokenKey.get("value"));
		}
		return tokenKeys;
	}

	/**
	 * Return the URL of the UAA.
	 * @return the UAA url
	 */
	public String getUaaUrl() {
		if (this.uaaUrl == null) {
			try {
				Map<?, ?> response = this.restTemplate
						.getForObject(this.cloudControllerUrl + "/info", Map.class);
				this.uaaUrl = (String) response.get("token_endpoint");
			}
			catch (HttpStatusCodeException ex) {
				throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Unable to fetch token keys from UAA");
			}
		}
		return this.uaaUrl;
	}

}
