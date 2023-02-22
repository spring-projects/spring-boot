/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.info;

import org.springframework.boot.actuate.autoconfigure.OnEndpointElementCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * {@link Condition} that checks if an info indicator is enabled.
 *
 * @author Stephane Nicoll
 */
class OnEnabledInfoContributorCondition extends OnEndpointElementCondition {

	OnEnabledInfoContributorCondition() {
		super("management.info.", ConditionalOnEnabledInfoContributor.class);
	}

	@Override
	protected ConditionOutcome getDefaultOutcome(ConditionContext context, AnnotationAttributes annotationAttributes) {
		InfoContributorFallback fallback = annotationAttributes.getEnum("fallback");
		if (fallback == InfoContributorFallback.DISABLE) {
			return new ConditionOutcome(false, ConditionMessage.forCondition(ConditionalOnEnabledInfoContributor.class)
				.because("management.info." + annotationAttributes.getString("value") + ".enabled is not true"));
		}
		return super.getDefaultOutcome(context, annotationAttributes);
	}

}
