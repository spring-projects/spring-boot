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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A condition that checks if an endpoint is enabled.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see ConditionalOnEnabledEndpoint
 */
class OnEnabledEndpointCondition extends SpringBootCondition {

	private static final String ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		AnnotationAttributes attributes = getEndpointAttributes(context, metadata);
		String id = attributes.getString("id");
		String key = "management.endpoint." + id + ".enabled";
		Boolean userDefinedEnabled = context.getEnvironment().getProperty(key,
				Boolean.class);
		if (userDefinedEnabled != null) {
			return new ConditionOutcome(userDefinedEnabled,
					ConditionMessage.forCondition(ConditionalOnEnabledEndpoint.class)
							.because("found property " + key + " with value "
									+ userDefinedEnabled));
		}
		Boolean userDefinedDefault = context.getEnvironment()
				.getProperty(ENABLED_BY_DEFAULT_KEY, Boolean.class);
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

	private AnnotationAttributes getEndpointAttributes(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Assert.state(
				metadata instanceof MethodMetadata
						&& metadata.isAnnotated(Bean.class.getName()),
				"OnEnabledEndpointCondition may only be used on @Bean methods");
		return getEndpointAttributes(context, (MethodMetadata) metadata);
	}

	private AnnotationAttributes getEndpointAttributes(ConditionContext context,
			MethodMetadata metadata) {
		// We should be safe to load at this point since we are in the REGISTER_BEAN phase
		try {
			Class<?> returnType = ClassUtils.forName(metadata.getReturnTypeName(),
					context.getClassLoader());
			return getEndpointAttributes(returnType);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to extract endpoint id for "
					+ metadata.getDeclaringClassName() + "." + metadata.getMethodName(),
					ex);
		}
	}

	protected AnnotationAttributes getEndpointAttributes(Class<?> type) {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.findMergedAnnotationAttributes(type, Endpoint.class, true, true);
		if (attributes == null) {
			attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(type,
					EndpointExtension.class, false, true);
			if (attributes != null) {
				return getEndpointAttributes(attributes.getClass("endpoint"));
			}
		}
		Assert.state(attributes != null,
				"OnEnabledEndpointCondition may only be used on @Bean methods that "
						+ "return an @Endpoint or and @EndpointExtension");
		return attributes;
	}

}
