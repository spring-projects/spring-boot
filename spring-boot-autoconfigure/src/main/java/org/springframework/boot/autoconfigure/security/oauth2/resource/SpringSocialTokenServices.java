/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
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
 * {@link ResourceServerTokenServices} backed by Spring Social.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class SpringSocialTokenServices implements ResourceServerTokenServices {

	private final OAuth2ConnectionFactory<?> connectionFactory;

	private final String clientId;

	public SpringSocialTokenServices(OAuth2ConnectionFactory<?> connectionFactory,
			String clientId) {
		this.connectionFactory = connectionFactory;
		this.clientId = clientId;
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken)
			throws AuthenticationException, InvalidTokenException {
		AccessGrant accessGrant = new AccessGrant(accessToken);
		Connection<?> connection = this.connectionFactory.createConnection(accessGrant);
		UserProfile user = connection.fetchUserProfile();
		return extractAuthentication(user);
	}

	private OAuth2Authentication extractAuthentication(UserProfile user) {
		String principal = user.getUsername();
		List<GrantedAuthority> authorities = AuthorityUtils
				.commaSeparatedStringToAuthorityList("ROLE_USER");
		OAuth2Request request = new OAuth2Request(null, this.clientId, null, true, null,
				null, null, null, null);
		return new OAuth2Authentication(request,
				new UsernamePasswordAuthenticationToken(principal, "N/A", authorities));
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

}
