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

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointEnablement;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointEnablementProvider;
import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointExtension;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A condition that checks if an endpoint is enabled.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class OnEnabledEndpointCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		EndpointAttributes attributes = getEndpointAttributes(context, metadata);
		EndpointEnablement endpointEnablement = attributes
				.getEnablement(new EndpointEnablementProvider(context.getEnvironment()));
		return new ConditionOutcome(endpointEnablement.isEnabled(),
				ConditionMessage.forCondition(ConditionalOnEnabledEndpoint.class)
						.because(endpointEnablement.getReason()));
	}

	private EndpointAttributes getEndpointAttributes(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Assert.state(
				metadata instanceof MethodMetadata
						&& metadata.isAnnotated(Bean.class.getName()),
				"OnEnabledEndpointCondition may only be used on @Bean methods");
		return getEndpointAttributes(context, (MethodMetadata) metadata);
	}

	private EndpointAttributes getEndpointAttributes(ConditionContext context,
			MethodMetadata methodMetadata) {
		try {
			// We should be safe to load at this point since we are in the
			// REGISTER_BEAN phase
			Class<?> returnType = ClassUtils.forName(methodMetadata.getReturnTypeName(),
					context.getClassLoader());
			return extractEndpointAttributes(returnType);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to extract endpoint id for "
					+ methodMetadata.getDeclaringClassName() + "."
					+ methodMetadata.getMethodName(), ex);
		}
	}

	protected EndpointAttributes extractEndpointAttributes(Class<?> type) {
		EndpointAttributes attributes = extractEndpointAttributesFromEndpoint(type);
		if (attributes != null) {
			return attributes;
		}
		JmxEndpointExtension jmxExtension = AnnotationUtils.findAnnotation(type,
				JmxEndpointExtension.class);
		if (jmxExtension != null) {
			return extractEndpointAttributes(jmxExtension.endpoint());
		}
		WebEndpointExtension webExtension = AnnotationUtils.findAnnotation(type,
				WebEndpointExtension.class);
		if (webExtension != null) {
			return extractEndpointAttributes(webExtension.endpoint());
		}
		throw new IllegalStateException(
				"OnEnabledEndpointCondition may only be used on @Bean methods that return"
						+ " @Endpoint, @JmxEndpointExtension, or @WebEndpointExtension");
	}

	private EndpointAttributes extractEndpointAttributesFromEndpoint(
			Class<?> endpointClass) {
		Endpoint endpoint = AnnotationUtils.findAnnotation(endpointClass, Endpoint.class);
		if (endpoint == null) {
			return null;
		}
		// If both types are set, all exposure technologies are exposed
		EndpointExposure[] exposures = endpoint.exposure();
		return new EndpointAttributes(endpoint.id(), endpoint.defaultEnablement(),
				(exposures.length == 1 ? exposures[0] : null));
	}

	private static class EndpointAttributes {

		private final String id;

		private final DefaultEnablement defaultEnablement;

		private final EndpointExposure exposure;

		EndpointAttributes(String id, DefaultEnablement defaultEnablement,
				EndpointExposure exposure) {
			if (!StringUtils.hasText(id)) {
				throw new IllegalStateException("Endpoint id could not be determined");
			}
			this.id = id;
			this.defaultEnablement = defaultEnablement;
			this.exposure = exposure;
		}

		public EndpointEnablement getEnablement(EndpointEnablementProvider provider) {
			return provider.getEndpointEnablement(this.id, this.defaultEnablement,
					this.exposure);
		}

	}

}
