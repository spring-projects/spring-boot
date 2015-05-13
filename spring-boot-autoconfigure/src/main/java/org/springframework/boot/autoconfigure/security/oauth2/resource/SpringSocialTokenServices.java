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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;

/**
 * @author Dave Syer
 *
 */
public class SpringSocialTokenServices implements ResourceServerTokenServices {

	protected final Log logger = LogFactory.getLog(getClass());

	private OAuth2ConnectionFactory<?> connectionFactory;

	private String clientId;

	public SpringSocialTokenServices(OAuth2ConnectionFactory<?> connectionFactory,
			String clientId) {
		this.connectionFactory = connectionFactory;
		this.clientId = clientId;
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken)
			throws AuthenticationException, InvalidTokenException {

		Connection<?> connection = connectionFactory.createConnection(new AccessGrant(
				accessToken));
		UserProfile user = connection.fetchUserProfile();
		return extractAuthentication(user);
	}

	private OAuth2Authentication extractAuthentication(UserProfile user) {
		UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
				user.getUsername(), "N/A",
				AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
		principal.setDetails(user);
		OAuth2Request request = new OAuth2Request(null, clientId, null, true, null, null,
				null, null, null);
		return new OAuth2Authentication(request, principal);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

}