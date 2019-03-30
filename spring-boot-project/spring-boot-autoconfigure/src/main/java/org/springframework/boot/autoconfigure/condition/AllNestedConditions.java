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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Condition;

/**
 * {@link Condition} that will match when all nested class conditions match. Can be used
 * to create composite conditions, for example:
 *
 * <pre class="code">
 * static class OnJndiAndProperty extends AllNestedConditions {
 *
 *    OnJndiAndProperty() {
 *        super(ConfigurationPhase.PARSE_CONFIGURATION);
 *    }
 *
 *    &#064;ConditionalOnJndi()
 *    static class OnJndi {
 *    }
 *
 *    &#064;ConditionalOnProperty("something")
 *    static class OnProperty {
 *    }
 *
 * }
 * </pre>
 * <p>
 * The
 * {@link org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase
 * ConfigurationPhase} should be specified according to the conditions that are defined.
 * In the example above, all conditions are static and can be evaluated early so
 * {@code PARSE_CONFIGURATION} is a right fit.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public abstract class AllNestedConditions extends AbstractNestedCondition {

	public AllNestedConditions(ConfigurationPhase configurationPhase) {
		super(configurationPhase);
	}

	@Override
	protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
		boolean match = hasSameSize(memberOutcomes.getMatches(), memberOutcomes.getAll());
		List<ConditionMessage> messages = new ArrayList<>();
		messages.add(ConditionMessage.forCondition("AllNestedConditions")
				.because(memberOutcomes.getMatches().size() + " matched "
						+ memberOutcomes.getNonMatches().size() + " did not"));
		for (ConditionOutcome outcome : memberOutcomes.getAll()) {
			messages.add(outcome.getConditionMessage());
		}
		return new ConditionOutcome(match, ConditionMessage.of(messages));
	}

	private boolean hasSameSize(List<?> list1, List<?> list2) {
		return list1.size() == list2.size();
	}

}
