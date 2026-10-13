/*
 * Copyright 2012-present the original author or authors.
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
