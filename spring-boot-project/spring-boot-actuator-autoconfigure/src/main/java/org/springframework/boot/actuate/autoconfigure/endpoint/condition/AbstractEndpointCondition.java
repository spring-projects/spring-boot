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

import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
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
 * Base class for {@link Endpoint} related {@link SpringBootCondition} implementations.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class AbstractEndpointCondition extends SpringBootCondition {

	AnnotationAttributes getEndpointAttributes(Class<?> annotationClass,
			ConditionContext context, AnnotatedTypeMetadata metadata) {
		return getEndpointAttributes(getEndpointType(annotationClass, context, metadata));
	}

	Class<?> getEndpointType(Class<?> annotationClass, ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(annotationClass.getName());
		if (attributes != null && attributes.containsKey("endpoint")) {
			Class<?> target = (Class<?>) attributes.get("endpoint");
			if (target != Void.class) {
				return target;
			}
		}
		Assert.state(
				metadata instanceof MethodMetadata
						&& metadata.isAnnotated(Bean.class.getName()),
				"EndpointCondition must be used on @Bean methods when the endpoint is not specified");
		MethodMetadata methodMetadata = (MethodMetadata) metadata;
		try {
			return ClassUtils.forName(methodMetadata.getReturnTypeName(),
					context.getClassLoader());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to extract endpoint id for "
					+ methodMetadata.getDeclaringClassName() + "."
					+ methodMetadata.getMethodName(), ex);
		}
	}

	AnnotationAttributes getEndpointAttributes(Class<?> type) {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.findMergedAnnotationAttributes(type, Endpoint.class, true, true);
		if (attributes != null) {
			return attributes;
		}
		attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(type,
				EndpointExtension.class, false, true);
		Assert.state(attributes != null,
				"No endpoint is specified and the return type of the @Bean method is "
						+ "neither an @Endpoint, nor an @EndpointExtension");
		return getEndpointAttributes(attributes.getClass("endpoint"));
	}

}
