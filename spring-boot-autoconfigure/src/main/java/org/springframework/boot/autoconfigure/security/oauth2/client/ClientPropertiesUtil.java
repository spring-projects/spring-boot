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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Internal class that provides utilities for searching the {@link Environment}
 * for OAuth 2.0 / OpenID Connect 1.0 client properties.
 *
 * @author Joe Grandja
 * @since 2.0.0
 */
final class ClientPropertiesUtil {

	static final String CLIENT_PROPERTY_PREFIX = "security.oauth2.client";

	static final String CLIENT_ID_PROPERTY = "client-id";

	static final String USER_INFO_URI_PROPERTY = "user-info-uri";

	static final String USER_NAME_ATTR_NAME_PROPERTY = "user-name-attribute-name";

	static Set<String> getClientKeys(Environment environment) {
		return getClientPropertiesByClient(environment).keySet();
	}

	static Map<String, Map> getClientPropertiesByClient(Environment environment) {
		Map<String, Object> clientPropertiesByClient = Binder.get(environment)
				.bind(CLIENT_PROPERTY_PREFIX, Bindable.mapOf(String.class, Object.class))
				.orElse(new HashMap<>());

		// Filter out clients that don't have the client-id property set
		return clientPropertiesByClient.entrySet().stream()
				.map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), (Map) e.getValue()))
				.filter(e -> e.getValue().containsKey(CLIENT_ID_PROPERTY))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}