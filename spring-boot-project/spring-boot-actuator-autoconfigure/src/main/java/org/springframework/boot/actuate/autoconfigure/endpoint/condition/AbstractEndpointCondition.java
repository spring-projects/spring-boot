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

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Base class for {@link Endpoint @Endpoint} related {@link SpringBootCondition}
 * implementations.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class AbstractEndpointCondition extends SpringBootCondition {

	private static final String ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default";

	private static final ConcurrentReferenceHashMap<Environment, Optional<Boolean>> enabledByDefaultCache = new ConcurrentReferenceHashMap<>();

	AnnotationAttributes getEndpointAttributes(Class<?> annotationClass, ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		return getEndpointAttributes(getEndpointType(annotationClass, context, metadata));
	}

	protected ConditionOutcome getEnablementOutcome(ConditionContext context, AnnotatedTypeMetadata metadata,
			Class<? extends Annotation> annotationClass) {
		Environment environment = context.getEnvironment();
		AnnotationAttributes attributes = getEndpointAttributes(annotationClass, context, metadata);
		EndpointId id = EndpointId.of(environment, attributes.getString("id"));
		String key = "management.endpoint." + id.toLowerCaseString() + ".enabled";
		Boolean userDefinedEnabled = environment.getProperty(key, Boolean.class);
		if (userDefinedEnabled != null) {
			return new ConditionOutcome(userDefinedEnabled, ConditionMessage.forCondition(annotationClass)
					.because("found property " + key + " with value " + userDefinedEnabled));
		}
		Boolean userDefinedDefault = isEnabledByDefault(environment);
		if (userDefinedDefault != null) {
			return new ConditionOutcome(userDefinedDefault, ConditionMessage.forCondition(annotationClass).because(
					"no property " + key + " found so using user defined default from " + ENABLED_BY_DEFAULT_KEY));
		}
		boolean endpointDefault = attributes.getBoolean("enableByDefault");
		return new ConditionOutcome(endpointDefault, ConditionMessage.forCondition(annotationClass)
				.because("no property " + key + " found so using endpoint default"));
	}

	protected Class<?> getEndpointType(Class<?> annotationClass, ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(annotationClass.getName());
		if (attributes != null && attributes.containsKey("endpoint")) {
			Class<?> target = (Class<?>) attributes.get("endpoint");
			if (target != Void.class) {
				return target;
			}
		}
		Assert.state(metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName()),
				"EndpointCondition must be used on @Bean methods when the endpoint is not specified");
		MethodMetadata methodMetadata = (MethodMetadata) metadata;
		try {
			return ClassUtils.forName(methodMetadata.getReturnTypeName(), context.getClassLoader());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to extract endpoint id for "
					+ methodMetadata.getDeclaringClassName() + "." + methodMetadata.getMethodName(), ex);
		}
	}

	protected AnnotationAttributes getEndpointAttributes(Class<?> type) {
		MergedAnnotations annotations = MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY);
		MergedAnnotation<Endpoint> endpoint = annotations.get(Endpoint.class);
		if (endpoint.isPresent()) {
			return endpoint.asAnnotationAttributes();
		}
		MergedAnnotation<EndpointExtension> extension = annotations.get(EndpointExtension.class);
		Assert.state(extension.isPresent(), "No endpoint is specified and the return type of the @Bean method is "
				+ "neither an @Endpoint, nor an @EndpointExtension");
		return getEndpointAttributes(extension.getClass("endpoint"));
	}

	private Boolean isEnabledByDefault(Environment environment) {
		Optional<Boolean> enabledByDefault = enabledByDefaultCache.get(environment);
		if (enabledByDefault == null) {
			enabledByDefault = Optional.ofNullable(environment.getProperty(ENABLED_BY_DEFAULT_KEY, Boolean.class));
			enabledByDefaultCache.put(environment, enabledByDefault);
		}
		return enabledByDefault.orElse(null);
	}

}
