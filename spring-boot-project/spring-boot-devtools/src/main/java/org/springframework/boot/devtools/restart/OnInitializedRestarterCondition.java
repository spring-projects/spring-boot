/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks that a {@link Restarter} is available and initialized.
 *
 * @author Phillip Webb
 * @see ConditionalOnInitializedRestarter
 */
class OnInitializedRestarterCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
				.forCondition("Initialized Restarter Condition");
		Restarter restarter = getRestarter();
		if (restarter == null) {
			return ConditionOutcome.noMatch(message.because("unavailable"));
		}
		if (restarter.getInitialUrls() == null) {
			return ConditionOutcome.noMatch(message.because("initialized without URLs"));
		}
		return ConditionOutcome.match(message.because("available and initialized"));
	}

	private Restarter getRestarter() {
		try {
			return Restarter.getInstance();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
