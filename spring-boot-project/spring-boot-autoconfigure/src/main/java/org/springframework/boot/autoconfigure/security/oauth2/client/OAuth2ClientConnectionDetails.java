/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish connections to a OAuth2 authentication server.
 *
 * @author Philipp Kessler
 * @since 3.3.0
 */
public interface OAuth2ClientConnectionDetails extends ConnectionDetails {

	default Map<String, Registration> getRegistrations() {
		return new HashMap<>();
	}

	default Map<String, Provider> getProviders() {
		return new HashMap<>();
	}

	interface Registration {

		/**
		 * Reference to the OAuth 2.0 provider to use. May reference an element from the
		 * 'provider' property or used one of the commonly used providers (google, github,
		 * facebook, okta).
		 * @return reference to the OAuth 2.0 provider to use.
		 */
		String getProvider();

		/**
		 * Client ID for the registration.
		 * @return client ID for the registration.
		 */
		String getClientId();

		/**
		 * Client secret of the registration.
		 * @return client secret of the registration.
		 */
		String getClientSecret();

		/**
		 * Client authentication method. May be left blank when using a pre-defined
		 * provider.
		 * @return client authentication method.
		 */
		String getClientAuthenticationMethod();

		/**
		 * Authorization grant type. May be left blank when using a pre-defined provider.
		 * @return authorization grant type.
		 */
		String getAuthorizationGrantType();

		/**
		 * Redirect URI. May be left blank when using a pre-defined provider.
		 * @return redirect URI. May be left blank when using a pre-defined provider.
		 */
		String getRedirectUri();

		/**
		 * Authorization scopes. When left blank the provider's default scopes, if any,
		 * will be used.
		 * @return authorization scopes.
		 */
		Set<String> getScopes();

		/**
		 * Client name. May be left blank when using a pre-defined provider.
		 * @return client name. May be left blank when using a pre-defined provider.
		 */
		String getClientName();

		static Registration of(String provider, String clientId, String clientSecret, String clientAuthenticationMethod,
				String authorizationGrantType, String redirectUri, Set<String> scope, String clientName) {
			return new Registration() {
				@Override
				public String getProvider() {
					return provider;
				}

				@Override
				public String getClientId() {
					return clientId;
				}

				@Override
				public String getClientSecret() {
					return clientSecret;
				}

				@Override
				public String getClientAuthenticationMethod() {
					return clientAuthenticationMethod;
				}

				@Override
				public String getAuthorizationGrantType() {
					return authorizationGrantType;
				}

				@Override
				public String getRedirectUri() {
					return redirectUri;
				}

				@Override
				public Set<String> getScopes() {
					return scope;
				}

				@Override
				public String getClientName() {
					return clientName;
				}
			};
		}

	}

	interface Provider {

		/**
		 * Authorization URI for the provider.
		 * @return authorization URI for the provider.
		 */
		default String getAuthorizationUri() {
			return null;
		}

		/**
		 * Token URI for the provider.
		 * @return token URI for the provider.
		 */
		default String getTokenUri() {
			return null;
		}

		/**
		 * User info URI for the provider.
		 * @return user info URI for the provider.
		 */
		default String getUserInfoUri() {
			return null;
		}

		/**
		 * User info authentication method for the provider.
		 * @return user info authentication method for the provider.
		 */
		default String getUserInfoAuthenticationMethod() {
			return null;
		}

		/**
		 * Name of the attribute that will be used to extract the username from the call
		 * to 'userInfoUri'.
		 * @return name of the attribute that will be used to extract the username from
		 * the call * to 'userInfoUri'
		 */
		default String getUserNameAttribute() {
			return null;
		}

		/**
		 * JWK set URI for the provider.
		 * @return jwk set URI for the provider.
		 */
		default String getJwkSetUri() {
			return null;
		}

		/**
		 * URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0
		 * Authorization Server Metadata endpoint defined by RFC 8414.
		 * @return uri that can either be an OpenID Connect discovery endpoint or an OAuth
		 * 2.0 * Authorization Server Metadata endpoint defined by RFC 8414.
		 */
		default String getIssuerUri() {
			return null;
		}

		static Provider of(String authorizationUri, String tokenUri, String userInfoUri,
				String userInfoAuthenticationMethod, String userNameAttributes, String jwkSetUri, String issuerUri) {
			return new Provider() {
				@Override
				public String getAuthorizationUri() {
					return authorizationUri;
				}

				@Override
				public String getTokenUri() {
					return tokenUri;
				}

				@Override
				public String getUserInfoUri() {
					return userInfoUri;
				}

				@Override
				public String getUserInfoAuthenticationMethod() {
					return userInfoAuthenticationMethod;
				}

				@Override
				public String getUserNameAttribute() {
					return userNameAttributes;
				}

				@Override
				public String getJwkSetUri() {
					return jwkSetUri;
				}

				@Override
				public String getIssuerUri() {
					return issuerUri;
				}
			};
		}

	}

}
