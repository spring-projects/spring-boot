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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Registration;
import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.core.convert.ConversionException;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Adapter class to convert {@link OAuth2ClientProperties} to a
 * {@link ClientRegistration}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
final class OAuth2ClientPropertiesRegistrationAdapter {

	private OAuth2ClientPropertiesRegistrationAdapter() {
	}

	public static Map<String, ClientRegistration> getClientRegistrations(
			OAuth2ClientProperties properties) {
		Map<String, ClientRegistration> clientRegistrations = new HashMap<>();
		properties.getRegistration().forEach((key, value) -> clientRegistrations.put(key,
				getClientRegistration(key, value, properties.getProvider())));
		return clientRegistrations;
	}

	private static ClientRegistration getClientRegistration(String registrationId,
			Registration properties, Map<String, Provider> providers) {
		Builder builder = getBuilder(registrationId, properties.getProvider(), providers);
		copyIfNotNull(properties::getClientId, builder::clientId);
		copyIfNotNull(properties::getClientSecret, builder::clientSecret);
		copyIfNotNull(properties::getClientAuthenticationMethod,
				builder::clientAuthenticationMethod, ClientAuthenticationMethod::new);
		copyIfNotNull(properties::getAuthorizationGrantType,
				builder::authorizationGrantType, AuthorizationGrantType::new);
		copyIfNotNull(properties::getRedirectUri, builder::redirectUri);
		copyIfNotNull(properties::getScope, builder::scope,
				(scope) -> scope.toArray(new String[scope.size()]));
		copyIfNotNull(properties::getClientName, builder::clientName);
		return builder.build();
	}

	private static Builder getBuilder(String registrationId, String configuredProviderId,
			Map<String, Provider> providers) {
		String providerId = (configuredProviderId == null ? registrationId
				: configuredProviderId);
		CommonOAuth2Provider provider = getCommonProvider(providerId);
		if (provider == null && !providers.containsKey(providerId)) {
			throw new IllegalStateException(
					getErrorMessage(configuredProviderId, registrationId));
		}
		Builder builder = (provider != null ? provider.getBuilder(registrationId)
				: ClientRegistration.withRegistrationId(registrationId));
		if (providers.containsKey(providerId)) {
			return getBuilder(builder, providers.get(providerId));
		}
		return builder;
	}

	private static String getErrorMessage(String configuredProviderId,
			String registrationId) {
		return (configuredProviderId == null
				? "Provider ID must be specified for client registration '"
						+ registrationId + "'"
				: "Unknown provider ID '" + configuredProviderId + "'");
	}

	private static Builder getBuilder(Builder builder, Provider provider) {
		copyIfNotNull(provider::getAuthorizationUri, builder::authorizationUri);
		copyIfNotNull(provider::getTokenUri, builder::tokenUri);
		copyIfNotNull(provider::getUserInfoUri, builder::userInfoUri);
		copyIfNotNull(provider::getJwkSetUri, builder::jwkSetUri);
		return builder;
	}

	private static CommonOAuth2Provider getCommonProvider(String providerId) {
		try {
			return new BinderConversionService(null).convert(providerId,
					CommonOAuth2Provider.class);
		}
		catch (ConversionException ex) {
			return null;
		}
	}

	private static <T> void copyIfNotNull(Supplier<T> supplier, Consumer<T> consumer) {
		copyIfNotNull(supplier, consumer, Function.identity());
	}

	private static <S, C> void copyIfNotNull(Supplier<S> supplier, Consumer<C> consumer,
			Function<S, C> converter) {
		S value = supplier.get();
		if (value != null) {
			consumer.accept(converter.apply(value));
		}
	}

}
