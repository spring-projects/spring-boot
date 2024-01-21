/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Adapts {@link OAuth2ClientProperties} to {@link OAuth2ClientConnectionDetails}.
 *
 * @author Philipp Kessler
 * @since 3.3.0
 */
public class PropertiesOAuth2ClientConnectionDetails implements OAuth2ClientConnectionDetails {

	private final OAuth2ClientProperties properties;

	public PropertiesOAuth2ClientConnectionDetails(OAuth2ClientProperties properties) {
		this.properties = properties;
	}

	@Override
	public Map<String, Registration> getRegistrations() {
		return this.properties.getRegistration()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Entry::getKey, (entry) -> {
				OAuth2ClientProperties.Registration registration = entry.getValue();
				return Registration.of(registration.getProvider(), registration.getClientId(),
						registration.getClientSecret(), registration.getClientAuthenticationMethod(),
						registration.getAuthorizationGrantType(), registration.getRedirectUri(),
						registration.getScope(), registration.getClientName());
			}));
	}

	@Override
	public Map<String, Provider> getProviders() {
		return this.properties.getProvider().entrySet().stream().collect(Collectors.toMap(Entry::getKey, (entry) -> {
			OAuth2ClientProperties.Provider provider = entry.getValue();
			return Provider.of(provider.getAuthorizationUri(), provider.getTokenUri(), provider.getUserInfoUri(),
					provider.getUserInfoAuthenticationMethod(), provider.getUserNameAttribute(),
					provider.getJwkSetUri(), provider.getIssuerUri());
		}));
	}

}
