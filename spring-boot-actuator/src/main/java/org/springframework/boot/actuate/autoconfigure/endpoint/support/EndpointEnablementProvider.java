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

package org.springframework.boot.actuate.autoconfigure.endpoint.support;

import org.springframework.boot.endpoint.EndpointExposure;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Determines an endpoint's enablement based on the current {@link Environment}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class EndpointEnablementProvider {

	private final Environment environment;

	/**
	 * Creates a new instance with the {@link Environment} to use.
	 * @param environment the environment
	 */
	public EndpointEnablementProvider(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@link EndpointEnablement} of an endpoint with no specific tech
	 * exposure.
	 * @param endpointId the id of the endpoint
	 * @param enabledByDefault whether the endpoint is enabled by default or not
	 * @return the {@link EndpointEnablement} of that endpoint
	 */
	public EndpointEnablement getEndpointEnablement(String endpointId,
			boolean enabledByDefault) {
		return getEndpointEnablement(endpointId, enabledByDefault, null);
	}

	/**
	 * Return the {@link EndpointEnablement} of an endpoint for a specific tech exposure.
	 * @param endpointId the id of the endpoint
	 * @param enabledByDefault whether the endpoint is enabled by default or not
	 * @param exposure the requested {@link EndpointExposure}
	 * @return the {@link EndpointEnablement} of that endpoint for the specified
	 * {@link EndpointExposure}
	 */
	public EndpointEnablement getEndpointEnablement(String endpointId,
			boolean enabledByDefault, EndpointExposure exposure) {
		Assert.hasText(endpointId, "Endpoint id must have a value");
		Assert.isTrue(!endpointId.equals("default"),
				"Endpoint id 'default' is a reserved "
						+ "value and cannot be used by an endpoint");
		EndpointEnablement result = findEnablement(endpointId, exposure);
		if (result != null) {
			return result;
		}
		result = findEnablement(getKey(endpointId, "enabled"));
		if (result != null) {
			return result;
		}
		// All endpoints specific attributes have been looked at. Checking default value
		// for the endpoint
		if (!enabledByDefault) {
			return getDefaultEndpointEnablement(endpointId, false, exposure);
		}
		return getGlobalEndpointEnablement(endpointId, enabledByDefault, exposure);
	}

	private EndpointEnablement findEnablement(String endpointId,
			EndpointExposure exposure) {
		if (exposure != null) {
			return findEnablement(getKey(endpointId, exposure));
		}
		return findEnablementForAnyExposureTechnology(endpointId);
	}

	private EndpointEnablement getGlobalEndpointEnablement(String endpointId,
			boolean enabledByDefault, EndpointExposure exposure) {
		EndpointEnablement result = findGlobalEndpointEnablement(exposure);
		if (result != null) {
			return result;
		}
		result = findEnablement(getKey("default", "enabled"));
		if (result != null) {
			return result;
		}
		return getDefaultEndpointEnablement(endpointId, enabledByDefault, exposure);
	}

	private EndpointEnablement findGlobalEndpointEnablement(EndpointExposure exposure) {
		if (exposure != null) {
			EndpointEnablement result = findEnablement(getKey("default", exposure));
			if (result != null) {
				return result;
			}
			if (!exposure.isEnabledByDefault()) {
				return getDefaultEndpointEnablement("default", false, exposure);
			}
			return null;
		}
		return findEnablementForAnyExposureTechnology("default");
	}

	private EndpointEnablement findEnablementForAnyExposureTechnology(String endpointId) {
		for (EndpointExposure candidate : EndpointExposure.values()) {
			EndpointEnablement result = findEnablementForExposureTechnology(endpointId,
					candidate);
			if (result != null && result.isEnabled()) {
				return result;
			}
		}
		return null;
	}

	private EndpointEnablement findEnablementForExposureTechnology(String endpointId,
			EndpointExposure exposure) {
		String endpointTypeKey = getKey(endpointId, exposure);
		return findEnablement(endpointTypeKey);
	}

	private EndpointEnablement getDefaultEndpointEnablement(String endpointId,
			boolean enabledByDefault, EndpointExposure exposure) {
		return new EndpointEnablement(enabledByDefault,
				createDefaultEnablementMessage(endpointId, enabledByDefault, exposure));
	}

	private String createDefaultEnablementMessage(String endpointId,
			boolean enabledByDefault, EndpointExposure exposure) {
		StringBuilder message = new StringBuilder();
		message.append(String.format("endpoint '%s' ", endpointId));
		if (exposure != null) {
			message.append(String.format("(%s) ", exposure.name().toLowerCase()));
		}
		message.append(String.format("is %s by default",
				(enabledByDefault ? "enabled" : "disabled")));
		return message.toString();
	}

	private String getKey(String endpointId, EndpointExposure exposure) {
		return getKey(endpointId, exposure.name().toLowerCase() + ".enabled");
	}

	private String getKey(String endpointId, String suffix) {
		return "endpoints." + endpointId + "." + suffix;
	}

	/**
	 * Return an {@link EndpointEnablement} for the specified key if it is set or
	 * {@code null} if the key is not present in the environment.
	 * @param key the key to check
	 * @return the outcome or {@code null} if the key is no set
	 */
	private EndpointEnablement findEnablement(String key) {
		if (this.environment.containsProperty(key)) {
			boolean match = this.environment.getProperty(key, Boolean.class, true);
			return new EndpointEnablement(match, String.format("found property %s", key));
		}
		return null;
	}

}
