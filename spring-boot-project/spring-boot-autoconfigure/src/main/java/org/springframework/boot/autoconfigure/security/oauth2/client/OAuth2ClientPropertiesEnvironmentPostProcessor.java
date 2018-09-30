/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * {@link EnvironmentPostProcessor} that migrates legacy OAuth2 login client properties
 * under the `spring.security.oauth2.client.login` prefix.
 *
 * @author Madhura Bhave
 * @since 2.1.0
 */
public class OAuth2ClientPropertiesEnvironmentPostProcessor
		implements EnvironmentPostProcessor, Ordered {

	private static final Bindable<Map<String, OAuth2ClientProperties.LoginClientRegistration>> STRING_LEGACY_REGISTRATION_MAP = Bindable
			.mapOf(String.class, OAuth2ClientProperties.LoginClientRegistration.class);

	private static final String PREFIX = "spring.security.oauth2.client.registration";

	private static final String LOGIN_REGISTRATION_PREFIX = PREFIX + ".login.";

	private static final String UPDATED_PROPERTY_SOURCE_SUFFIX = "-updated-oauth-client";

	private int order = ConfigFileApplicationListener.DEFAULT_ORDER + 1;

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		environment.getPropertySources().forEach((propertySource) -> {
			String name = propertySource.getName();
			Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources
					.from(propertySource);
			ConfigurationPropertySource source = sources.iterator().next();
			Binder binder = new Binder(sources);
			Map<String, Object> map = new LinkedHashMap<>();
			MapPropertySource updatedPropertySource = new MapPropertySource(
					name + UPDATED_PROPERTY_SOURCE_SUFFIX, map);
			Map<String, OAuth2ClientProperties.LoginClientRegistration> registrations = binder
					.bind(PREFIX, STRING_LEGACY_REGISTRATION_MAP)
					.orElse(Collections.emptyMap());
			registrations.entrySet()
					.forEach((entry) -> addProperties(entry, source, map));
			if (!map.isEmpty()) {
				environment.getPropertySources().addBefore(name, updatedPropertySource);
			}
		});
	}

	private void addProperties(
			Map.Entry<String, OAuth2ClientProperties.LoginClientRegistration> entry,
			ConfigurationPropertySource source, Map<String, Object> map) {
		OAuth2ClientProperties.LoginClientRegistration registration = entry.getValue();
		String registrationId = entry.getKey();
		addProperty(registrationId, "client-id", registration::getClientId, map, source);
		addProperty(registrationId, "client-secret", registration::getClientSecret, map,
				source);
		addProperty(registrationId, "client-name", registration::getClientName, map,
				source);
		addProperty(registrationId, "redirect-uri-template", registration::getRedirectUri,
				map, source);
		addProperty(registrationId, "authorization-grant-type",
				registration::getAuthorizationGrantType, map, source);
		addProperty(registrationId, "client-authentication-method",
				registration::getClientAuthenticationMethod, map, source);
		addProperty(registrationId, "provider", registration::getProvider, map, source);
		addProperty(registrationId, "scope", registration::getScope, map, source);
	}

	private void addProperty(String registrationId, String property,
			Supplier<Object> valueSupplier, Map<String, Object> map,
			ConfigurationPropertySource source) {
		String registrationKey = PREFIX + "." + registrationId + ".";
		String loginRegistrationKey = LOGIN_REGISTRATION_PREFIX + registrationId + ".";
		if (source.getConfigurationProperty(
				ConfigurationPropertyName.of(registrationKey + property)) != null) {
			map.put(loginRegistrationKey + property, valueSupplier.get());
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
