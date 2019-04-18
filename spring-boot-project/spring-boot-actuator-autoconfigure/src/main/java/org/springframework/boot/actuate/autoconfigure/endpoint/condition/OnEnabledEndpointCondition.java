/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.util.Optional;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A condition that checks if an endpoint is enabled.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see ConditionalOnEnabledEndpoint
 */
class OnEnabledEndpointCondition extends AbstractEndpointCondition {

	private static final String ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default";

	private static final ConcurrentReferenceHashMap<Environment, Optional<Boolean>> enabledByDefaultCache = new ConcurrentReferenceHashMap<>();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		AnnotationAttributes attributes = getEndpointAttributes(
				ConditionalOnEnabledEndpoint.class, context, metadata);
		EndpointId id = EndpointId.of(attributes.getString("id"));
		String key = "management.endpoint." + id.toLowerCaseString() + ".enabled";
		Boolean userDefinedEnabled = environment.getProperty(key, Boolean.class);
		if (userDefinedEnabled != null) {
			return new ConditionOutcome(userDefinedEnabled,
					ConditionMessage.forCondition(ConditionalOnEnabledEndpoint.class)
							.because("found property " + key + " with value "
									+ userDefinedEnabled));
		}
		Boolean userDefinedDefault = isEnabledByDefault(environment);
		if (userDefinedDefault != null) {
			return new ConditionOutcome(userDefinedDefault,
					ConditionMessage.forCondition(ConditionalOnEnabledEndpoint.class)
							.because("no property " + key
									+ " found so using user defined default from "
									+ ENABLED_BY_DEFAULT_KEY));
		}
		boolean endpointDefault = attributes.getBoolean("enableByDefault");
		return new ConditionOutcome(endpointDefault,
				ConditionMessage.forCondition(ConditionalOnEnabledEndpoint.class).because(
						"no property " + key + " found so using endpoint default"));
	}

	private Boolean isEnabledByDefault(Environment environment) {
		Optional<Boolean> enabledByDefault = enabledByDefaultCache.get(environment);
		if (enabledByDefault == null) {
			enabledByDefault = Optional.ofNullable(
					environment.getProperty(ENABLED_BY_DEFAULT_KEY, Boolean.class));
			enabledByDefaultCache.put(environment, enabledByDefault);
		}
		return enabledByDefault.orElse(null);
	}

}
