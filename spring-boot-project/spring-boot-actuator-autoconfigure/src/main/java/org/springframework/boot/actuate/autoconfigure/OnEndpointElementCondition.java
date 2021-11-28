/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Base endpoint element condition. An element can be disabled globally via the
 * {@code defaults} name or individually via the name of the element.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 2.0.0
 */
public abstract class OnEndpointElementCondition extends SpringBootCondition {

	private final String prefix;

	private final Class<? extends Annotation> annotationType;

	protected OnEndpointElementCondition(String prefix, Class<? extends Annotation> annotationType) {
		this.prefix = prefix;
		this.annotationType = annotationType;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(this.annotationType.getName()));
		String endpointName = annotationAttributes.getString("value");
		ConditionOutcome outcome = getEndpointOutcome(context, endpointName);
		if (outcome != null) {
			return outcome;
		}
		return getDefaultOutcome(context, annotationAttributes);
	}

	protected ConditionOutcome getEndpointOutcome(ConditionContext context, String endpointName) {
		Environment environment = context.getEnvironment();
		String enabledProperty = this.prefix + endpointName + ".enabled";
		if (environment.containsProperty(enabledProperty)) {
			boolean match = environment.getProperty(enabledProperty, Boolean.class, true);
			return new ConditionOutcome(match, ConditionMessage.forCondition(this.annotationType)
					.because(this.prefix + endpointName + ".enabled is " + match));
		}
		return null;
	}

	/**
	 * Return the default outcome that should be used if property is not set. By default
	 * this method will use the {@code <prefix>.defaults.enabled} property, matching if it
	 * is {@code true} or if it is not configured.
	 * @param context the condition context
	 * @param annotationAttributes the annotation attributes
	 * @return the default outcome
	 * @since 2.6.0
	 */
	protected ConditionOutcome getDefaultOutcome(ConditionContext context, AnnotationAttributes annotationAttributes) {
		return getDefaultEndpointsOutcome(context);
	}

	/**
	 * Return the default outcome that should be used.
	 * @param context the condition context
	 * @return the default outcome
	 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
	 * {@link #getDefaultOutcome(ConditionContext, AnnotationAttributes)}
	 */
	@Deprecated
	protected ConditionOutcome getDefaultEndpointsOutcome(ConditionContext context) {
		boolean match = Boolean
				.parseBoolean(context.getEnvironment().getProperty(this.prefix + "defaults.enabled", "true"));
		return new ConditionOutcome(match, ConditionMessage.forCondition(this.annotationType)
				.because(this.prefix + "defaults.enabled is considered " + match));
	}

}
