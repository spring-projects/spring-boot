/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link SpringBootCondition} to check whether gRPC server/service is enabled.
 *
 * @author Chris Bono
 * @see ConditionalOnGrpcServerEnabled
 */
class OnEnabledGrpcServerCondition extends SpringBootCondition {

	private static final String SERVER_PROPERTY = "spring.grpc.server.enabled";

	private static final String SERVICE_PROPERTY = "spring.grpc.server.%s.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Boolean serverEnabled = context.getEnvironment().getProperty(SERVER_PROPERTY, Boolean.class);
		if (serverEnabled != null && !serverEnabled) {
			return new ConditionOutcome(serverEnabled,
					ConditionMessage.forCondition(ConditionalOnGrpcServerEnabled.class)
						.because(SERVER_PROPERTY + " is " + serverEnabled));
		}
		String serviceName = getServiceName(metadata);
		if (StringUtils.hasLength(serviceName)) {
			Boolean serviceEnabled = context.getEnvironment()
				.getProperty(SERVICE_PROPERTY.formatted(serviceName), Boolean.class);
			if (serviceEnabled != null) {
				return new ConditionOutcome(serviceEnabled,
						ConditionMessage.forCondition(ConditionalOnGrpcServerEnabled.class)
							.because(SERVICE_PROPERTY.formatted(serviceName) + " is " + serviceEnabled));
			}
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnGrpcServerEnabled.class)
			.because("server and service are enabled by default"));
	}

	private static @Nullable String getServiceName(AnnotatedTypeMetadata metadata) {
		Map<String, @Nullable Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnGrpcServerEnabled.class.getName());
		if (attributes == null) {
			return null;
		}
		return (String) attributes.get("value");
	}

}
