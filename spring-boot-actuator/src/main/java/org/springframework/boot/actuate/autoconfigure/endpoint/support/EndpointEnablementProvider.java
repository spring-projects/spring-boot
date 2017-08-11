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

import org.springframework.boot.endpoint.EndpointType;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

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
	 * @param endpointType the requested {@link EndpointType}
	 * @return the {@link EndpointEnablement} of that endpoint for the specified
	 * {@link EndpointType}
	 */
	public EndpointEnablement getEndpointEnablement(String endpointId,
			boolean enabledByDefault, EndpointType endpointType) {
		if (!StringUtils.hasText(endpointId)) {
			throw new IllegalArgumentException("Endpoint id must have a value");
		}
		if (endpointId.equals("all")) {
			throw new IllegalArgumentException("Endpoint id 'all' is a reserved value "
					+ "and cannot be used by an endpoint");
		}

		if (endpointType != null) {
			String endpointTypeKey = createTechSpecificKey(endpointId, endpointType);
			EndpointEnablement endpointTypeSpecificOutcome = getEnablementFor(
					endpointTypeKey);
			if (endpointTypeSpecificOutcome != null) {
				return endpointTypeSpecificOutcome;
			}
		}
		else {
			// If any tech specific is on at this point we should enable the endpoint
			EndpointEnablement anyTechSpecificOutcome = getAnyTechSpecificOutcomeFor(
					endpointId);
			if (anyTechSpecificOutcome != null) {
				return anyTechSpecificOutcome;
			}
		}
		String endpointKey = createKey(endpointId, "enabled");
		EndpointEnablement endpointSpecificOutcome = getEnablementFor(endpointKey);
		if (endpointSpecificOutcome != null) {
			return endpointSpecificOutcome;
		}

		// All endpoints specific attributes have been looked at. Checking default value
		// for the endpoint
		if (!enabledByDefault) {
			return new EndpointEnablement(false, createDefaultEnablementMessage(
					endpointId, enabledByDefault, endpointType));
		}

		if (endpointType != null) {
			String globalTypeKey = createTechSpecificKey("all", endpointType);
			EndpointEnablement globalTypeOutcome = getEnablementFor(globalTypeKey);
			if (globalTypeOutcome != null) {
				return globalTypeOutcome;
			}
		}
		else {
			// Check if there is a global tech required
			EndpointEnablement anyTechGeneralOutcome = getAnyTechSpecificOutcomeFor(
					"all");
			if (anyTechGeneralOutcome != null) {
				return anyTechGeneralOutcome;
			}
		}

		String globalKey = createKey("all", "enabled");
		EndpointEnablement globalOutCome = getEnablementFor(globalKey);
		if (globalOutCome != null) {
			return globalOutCome;
		}
		return new EndpointEnablement(enabledByDefault, createDefaultEnablementMessage(
				endpointId, enabledByDefault, endpointType));
	}

	private String createDefaultEnablementMessage(String endpointId,
			boolean enabledByDefault, EndpointType endpointType) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("endpoint '%s' ", endpointId));
		if (endpointType != null) {
			sb.append(String.format("(%s) ", endpointType.name().toLowerCase()));
		}
		sb.append(String.format("is %s by default",
				(enabledByDefault ? "enabled" : "disabled")));
		return sb.toString();
	}

	private EndpointEnablement getAnyTechSpecificOutcomeFor(String endpointId) {
		for (EndpointType endpointType : EndpointType.values()) {
			String key = createTechSpecificKey(endpointId, endpointType);
			EndpointEnablement outcome = getEnablementFor(key);
			if (outcome != null && outcome.isEnabled()) {
				return outcome;
			}
		}
		return null;
	}

	private String createTechSpecificKey(String endpointId, EndpointType endpointType) {
		return createKey(endpointId, endpointType.name().toLowerCase() + ".enabled");
	}

	private String createKey(String endpointId, String suffix) {
		return "endpoints." + endpointId + "." + suffix;
	}

	/**
	 * Return an {@link EndpointEnablement} for the specified key if it is set or
	 * {@code null} if the key is not present in the environment.
	 * @param key the key to check
	 * @return the outcome or {@code null} if the key is no set
	 */
	private EndpointEnablement getEnablementFor(String key) {
		if (this.environment.containsProperty(key)) {
			boolean match = this.environment.getProperty(key, Boolean.class, true);
			return new EndpointEnablement(match, String.format("found property %s", key));
		}
		return null;
	}

}
