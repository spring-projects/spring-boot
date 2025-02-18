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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.core.env.PropertyResolver;

/**
 * {@link EndpointAccessResolver} that resolves the permitted level of access to an
 * endpoint using the following properties:
 * <ol>
 * <li>{@code management.endpoint.<id>.access} or {@code management.endpoint.<id>.enabled}
 * (deprecated)
 * <li>{@code management.endpoints.access.default} or
 * {@code management.endpoints.enabled-by-default} (deprecated)
 * </ol>
 * The resulting access is capped using {@code management.endpoints.access.max-permitted}.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public class PropertiesEndpointAccessResolver implements EndpointAccessResolver {

	private static final String DEFAULT_ACCESS_KEY = "management.endpoints.access.default";

	private static final String ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default";

	private final PropertyResolver properties;

	private final Access endpointsDefaultAccess;

	private final Access maxPermittedAccess;

	private final Map<EndpointId, Access> accessCache = new ConcurrentHashMap<>();

	public PropertiesEndpointAccessResolver(PropertyResolver properties) {
		this.properties = properties;
		this.endpointsDefaultAccess = determineDefaultAccess(properties);
		this.maxPermittedAccess = properties.getProperty("management.endpoints.access.max-permitted", Access.class,
				Access.UNRESTRICTED);
	}

	private static Access determineDefaultAccess(PropertyResolver properties) {
		Access defaultAccess = properties.getProperty(DEFAULT_ACCESS_KEY, Access.class);
		Boolean endpointsEnabledByDefault = properties.getProperty(ENABLED_BY_DEFAULT_KEY, Boolean.class);
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put(DEFAULT_ACCESS_KEY, defaultAccess);
			entries.put(ENABLED_BY_DEFAULT_KEY, endpointsEnabledByDefault);
		});
		if (defaultAccess != null) {
			return defaultAccess;
		}
		if (endpointsEnabledByDefault != null) {
			return endpointsEnabledByDefault ? org.springframework.boot.actuate.endpoint.Access.UNRESTRICTED
					: org.springframework.boot.actuate.endpoint.Access.NONE;
		}
		return null;
	}

	@Override
	public Access accessFor(EndpointId endpointId, Access defaultAccess) {
		return this.accessCache.computeIfAbsent(endpointId,
				(key) -> resolveAccess(endpointId.toLowerCaseString(), defaultAccess).cap(this.maxPermittedAccess));
	}

	private Access resolveAccess(String endpointId, Access defaultAccess) {
		String accessKey = "management.endpoint.%s.access".formatted(endpointId);
		String enabledKey = "management.endpoint.%s.enabled".formatted(endpointId);
		Access access = this.properties.getProperty(accessKey, Access.class);
		Boolean enabled = this.properties.getProperty(enabledKey, Boolean.class);
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put(accessKey, access);
			entries.put(enabledKey, enabled);
		});
		if (access != null) {
			return access;
		}
		if (enabled != null) {
			return (enabled) ? Access.UNRESTRICTED : Access.NONE;
		}
		return (this.endpointsDefaultAccess != null) ? this.endpointsDefaultAccess : defaultAccess;
	}

}
