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

import java.io.IOException;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Internal class that provides utilities for loading
 * default client properties from a well-known resource location.
 *
 * @author Joe Grandja
 * @since 2.0.0
 */
final class ClientPropertiesUtil {

	private static final String CLIENT_DEFAULTS_RESOURCE_LOCATION = "META-INF/spring-security-oauth2-client-defaults.properties";

	static final String CLIENT_REGISTRATIONS_PROPERTY_PREFIX = "spring.security.oauth2.client.registrations";

	private ClientPropertiesUtil() {
	}

	static PropertiesPropertySource loadDefaultsPropertySource() {
		try {
			Resource resource = new DefaultResourceLoader(ClientPropertiesUtil.class.getClassLoader())
					.getResource(CLIENT_DEFAULTS_RESOURCE_LOCATION);
			return new ResourcePropertySource(resource);
		}
		catch (IOException ioe) {
			throw new RuntimeException("Failed to load client defaults class path resource: "
					+ CLIENT_DEFAULTS_RESOURCE_LOCATION, ioe);
		}
	}
}
