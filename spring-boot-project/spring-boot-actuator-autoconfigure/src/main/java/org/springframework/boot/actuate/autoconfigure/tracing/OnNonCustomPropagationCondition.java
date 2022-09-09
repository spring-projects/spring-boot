/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if propagation is not {@link TracingProperties.Propagation.PropagationType#CUSTOM}.
 *
 * @author Marcin Grzejszczak
 */
class OnNonCustomPropagationCondition extends SpringBootCondition {

	private static final String PROPERTY_NAME = "management.tracing.propagation.type";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		TracingProperties.Propagation.PropagationType type = context.getEnvironment().getProperty(PROPERTY_NAME, TracingProperties.Propagation.PropagationType.class);
		return new ConditionOutcome(type != TracingProperties.Propagation.PropagationType.CUSTOM, ConditionMessage.forCondition("Tracing Propagation Type must not be custom")
				.because(PROPERTY_NAME + " is " + TracingProperties.Propagation.PropagationType.CUSTOM.name()));
	}

}
