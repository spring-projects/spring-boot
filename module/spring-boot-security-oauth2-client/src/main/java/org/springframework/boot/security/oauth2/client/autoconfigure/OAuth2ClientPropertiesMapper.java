/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.oauth2.client.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties.Provider;
import org.springframework.core.convert.ConversionException;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Maps {@link OAuth2ClientProperties} to {@link ClientRegistration ClientRegistrations}.
 *
 * @author Phillip Webb
 * @author Thiago Hirata
 * @author Madhura Bhave
 * @author MyeongHyeon Lee
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public final class OAuth2ClientPropertiesMapper {
	private static final boolean SPRING_SECURITY_CONFIG_PRESENT;
                                                                                          
	static {
		ClassLoader loader = OAuth2ClientPropertiesMapper.class.getClassLoader();
		SPRING_SECURITY_CONFIG_PRESENT = ClassUtils.isPresent("org.springframework.security.config.oauth2.client.CommonOAuth2Provider", loader);
	}

	private final OAuth2ClientProperties properties;

	/**
	 * Creates a new mapper for the given {@code properties}.
	 * @param properties the properties to map
	 */
	public OAuth2ClientPropertiesMapper(OAuth2ClientProperties properties) {
		this.properties = properties;
	}

	/**
	 * Maps the properties to {@link ClientRegistration ClientRegistrations}.
	 * @return the mapped {@code ClientRegistrations}
	 */
	public Map<String, ClientRegistration> asClientRegistrations() {
		Map<String, ClientRegistration> clientRegistrations = new HashMap<>();
		this.properties.getRegistration()
			.forEach((key, value) -> clientRegistrations.put(key,
					getClientRegistration(key, value, this.properties.getProvider())));
		return clientRegistrations;
	}

	private static ClientRegistration getClientRegistration(String registrationId,
			OAuth2ClientProperties.Registration properties, Map<String, Provider> providers) {
		Builder builder = getBuilderFromIssuerIfPossible(registrationId, properties.getProvider(), providers);
		if (builder == null) {
			builder = getBuilder(registrationId, properties.getProvider(), providers);
		}
		PropertyMapper map = PropertyMapper.get();
		map.from(properties::getClientId).to(builder::clientId);
		map.from(properties::getClientSecret).to(builder::clientSecret);
		map.from(properties::getClientAuthenticationMethod)
			.as(ClientAuthenticationMethod::new)
			.to(builder::clientAuthenticationMethod);
		map.from(properties::getAuthorizationGrantType)
			.as(AuthorizationGrantType::new)
			.to(builder::authorizationGrantType);
		map.from(properties::getRedirectUri).to(builder::redirectUri);
		map.from(properties::getScope).as(StringUtils::toStringArray).to(builder::scope);
		map.from(properties::getClientName).to(builder::clientName);
		return builder.build();
	}

	private static @Nullable Builder getBuilderFromIssuerIfPossible(String registrationId,
			@Nullable String configuredProviderId, Map<String, Provider> providers) {
		String providerId = (configuredProviderId != null) ? configuredProviderId : registrationId;
		if (providers.containsKey(providerId)) {
			Provider provider = providers.get(providerId);
			String issuer = provider.getIssuerUri();
			if (issuer != null) {
				Builder builder = ClientRegistrations.fromIssuerLocation(issuer).registrationId(registrationId);
				return getBuilder(builder, provider);
			}
		}
		return null;
	}

	private static Builder getBuilder(String registrationId, @Nullable String configuredProviderId,
			Map<String, Provider> providers) {
		String providerId = (configuredProviderId != null) ? configuredProviderId : registrationId;
		CommonOAuth2Provider provider = getCommonProvider(providerId);
		if (provider == null && !providers.containsKey(providerId)) {
			throw new IllegalStateException(getErrorMessage(configuredProviderId, registrationId));
		}
		Builder builder = (provider != null) ? provider.getBuilder(registrationId)
				: ClientRegistration.withRegistrationId(registrationId);
		if (providers.containsKey(providerId)) {
			return getBuilder(builder, providers.get(providerId));
		}
		return builder;
	}

	private static String getErrorMessage(@Nullable String configuredProviderId, String registrationId) {
		return ((configuredProviderId != null) ? "Unknown provider ID '" + configuredProviderId + "'"
				: "Provider ID must be specified for client registration '" + registrationId + "'");
	}

	private static Builder getBuilder(Builder builder, Provider provider) {
		PropertyMapper map = PropertyMapper.get();
		map.from(provider::getAuthorizationUri).to(builder::authorizationUri);
		map.from(provider::getTokenUri).to(builder::tokenUri);
		map.from(provider::getUserInfoUri).to(builder::userInfoUri);
		map.from(provider::getUserInfoAuthenticationMethod)
			.as(AuthenticationMethod::new)
			.to(builder::userInfoAuthenticationMethod);
		map.from(provider::getJwkSetUri).to(builder::jwkSetUri);
		map.from(provider::getUserNameAttribute).to(builder::userNameAttributeName);
		return builder;
	}

	private static @Nullable CommonOAuth2Provider getCommonProvider(String providerId) {
		if (!SPRING_SECURITY_CONFIG_PRESENT) {
			return null;
		}
		
		try {
			return ApplicationConversionService.getSharedInstance().convert(providerId, CommonOAuth2Provider.class);
		}
		catch (ConversionException ex) {
			return null;
		}
	}

}
