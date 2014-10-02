/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

public class UserInfoTokenServices implements ResourceServerTokenServices {

	protected final Log logger = LogFactory.getLog(getClass());

	private String userInfoEndpointUrl;

	private String clientId;

	private Collection<OAuth2RestOperations> resources = Collections.emptySet();

	public UserInfoTokenServices(String userInfoEndpointUrl, String clientId) {
		this.userInfoEndpointUrl = userInfoEndpointUrl;
		this.clientId = clientId;
	}

	public void setResources(Map<String, OAuth2RestOperations> resources) {
		this.resources = new ArrayList<OAuth2RestOperations>();
		for (Entry<String, OAuth2RestOperations> key : resources.entrySet()) {
			OAuth2RestOperations value = key.getValue();
			String clientIdForTemplate = value.getResource().getClientId();
			if (clientIdForTemplate!=null && clientIdForTemplate.equals(clientId)) {
				this.resources.add(value);
			}
		}
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken)
			throws AuthenticationException, InvalidTokenException {

		Map<String, Object> map = getMap(userInfoEndpointUrl, accessToken);

		if (map.containsKey("error")) {
			logger.debug("userinfo returned error: " + map.get("error"));
			throw new InvalidTokenException(accessToken);
		}

		return extractAuthentication(map);
	}

	private OAuth2Authentication extractAuthentication(Map<String, Object> map) {
		UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(
				getPrincipal(map), "N/A",
				AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
		user.setDetails(map);
		OAuth2Request request = new OAuth2Request(null, clientId, null, true, null, null,
				null, null, null);
		return new OAuth2Authentication(request, user);
	}

	private Object getPrincipal(Map<String, Object> map) {
		String[] keys = new String[] { "user", "username", "userid", "user_id", "login",
				"id" };
		for (String key : keys) {
			if (map.containsKey(key)) {
				return map.get(key);
			}
		}
		return "unknown";
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

	private Map<String, Object> getMap(String path, String accessToken) {
		logger.info("Getting user info from: " + path);
		OAuth2RestOperations restTemplate = null;
		for (OAuth2RestOperations candidate : resources) {
			try {
				if (accessToken.equals(candidate.getAccessToken().getValue())) {
					restTemplate = candidate;
				}
			}
			catch (Exception e) {
			}
		}
		if (restTemplate == null) {
			BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();
			resource.setClientId(clientId);
			restTemplate = new OAuth2RestTemplate(resource);
			restTemplate.getOAuth2ClientContext().setAccessToken(
					new DefaultOAuth2AccessToken(accessToken));
		}
		@SuppressWarnings("rawtypes")
		Map map = restTemplate.getForEntity(path, Map.class).getBody();
		@SuppressWarnings("unchecked")
		Map<String, Object> result = map;
		return result;
	}

}