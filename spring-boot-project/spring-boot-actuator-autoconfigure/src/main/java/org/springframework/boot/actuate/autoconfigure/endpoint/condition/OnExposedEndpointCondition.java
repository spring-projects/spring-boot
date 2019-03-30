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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A condition that checks if an endpoint is exposed.
 *
 * @author Brian Clozel
 * @see ConditionalOnExposedEndpoint
 */
class OnExposedEndpointCondition extends AbstractEndpointCondition {

	private static final String JMX_ENABLED_KEY = "spring.jmx.enabled";

	private static final ConcurrentReferenceHashMap<Environment, Set<ExposureInformation>> endpointExposureCache = new ConcurrentReferenceHashMap<>();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		if (CloudPlatform.CLOUD_FOUNDRY.isActive(environment)) {
			return new ConditionOutcome(true,
					ConditionMessage.forCondition(ConditionalOnExposedEndpoint.class)
							.because("application is running on Cloud Foundry"));
		}
		AnnotationAttributes attributes = getEndpointAttributes(
				ConditionalOnExposedEndpoint.class, context, metadata);
		EndpointId id = EndpointId.of(attributes.getString("id"));
		Set<ExposureInformation> exposureInformations = getExposureInformation(
				environment);
		for (ExposureInformation exposureInformation : exposureInformations) {
			if (exposureInformation.isExposed(id)) {
				return new ConditionOutcome(true,
						ConditionMessage.forCondition(ConditionalOnExposedEndpoint.class)
								.because("marked as exposed by a 'management.endpoints."
										+ exposureInformation.getPrefix()
										+ ".exposure' property"));
			}
		}
		return new ConditionOutcome(false,
				ConditionMessage.forCondition(ConditionalOnExposedEndpoint.class).because(
						"no 'management.endpoints' property marked it as exposed"));
	}

	private Set<ExposureInformation> getExposureInformation(Environment environment) {
		Set<ExposureInformation> exposureInformations = endpointExposureCache
				.get(environment);
		if (exposureInformations == null) {
			exposureInformations = new HashSet<>(2);
			Binder binder = Binder.get(environment);
			if (environment.getProperty(JMX_ENABLED_KEY, Boolean.class, false)) {
				exposureInformations.add(new ExposureInformation(binder, "jmx", "*"));
			}
			exposureInformations
					.add(new ExposureInformation(binder, "web", "info", "health"));
			endpointExposureCache.put(environment, exposureInformations);
		}
		return exposureInformations;
	}

	static class ExposureInformation {

		private final String prefix;

		private final Set<String> include;

		private final Set<String> exclude;

		private final Set<String> exposeDefaults;

		ExposureInformation(Binder binder, String prefix, String... exposeDefaults) {
			this.prefix = prefix;
			this.include = bind(binder,
					"management.endpoints." + prefix + ".exposure.include");
			this.exclude = bind(binder,
					"management.endpoints." + prefix + ".exposure.exclude");
			this.exposeDefaults = new HashSet<>(Arrays.asList(exposeDefaults));
		}

		private Set<String> bind(Binder binder, String name) {
			List<String> values = binder.bind(name, Bindable.listOf(String.class))
					.orElse(Collections.emptyList());
			Set<String> result = new HashSet<>(values.size());
			for (String value : values) {
				result.add("*".equals(value) ? "*"
						: EndpointId.fromPropertyValue(value).toLowerCaseString());
			}
			return result;
		}

		String getPrefix() {
			return this.prefix;
		}

		boolean isExposed(EndpointId endpointId) {
			String id = endpointId.toLowerCaseString();
			if (!this.exclude.isEmpty()) {
				if (this.exclude.contains("*") || this.exclude.contains(id)) {
					return false;
				}
			}
			if (this.include.isEmpty()) {
				if (this.exposeDefaults.contains("*")
						|| this.exposeDefaults.contains(id)) {
					return true;
				}
			}
			return this.include.contains("*") || this.include.contains(id);
		}

	}

}
