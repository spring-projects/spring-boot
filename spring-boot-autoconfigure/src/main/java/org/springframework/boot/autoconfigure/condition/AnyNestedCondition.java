/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link Condition} that will match when any nested class condition matches. Can be used
 * to create composite conditions, for example:
 *
 * <pre class="code">
 * static class OnJndiOrProperty extends AnyNestedCondition {
 *
 *    &#064;ConditionalOnJndi()
 *    static class OnJndi {
 *    }

 *    &#064;ConditionalOnProperty("something")
 *    static class OnProperty {
 *    }
 *
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public abstract class AnyNestedCondition extends AbstractNestedCondition {

	public AnyNestedCondition(ConfigurationPhase configurationPhase) {
		super(configurationPhase);
	}

	@Override
	protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
		return new ConditionOutcome(memberOutcomes.getMatches().size() > 0,
				"nested any match resulted in " + memberOutcomes.getMatches()
						+ " matches and " + memberOutcomes.getNonMatches()
						+ " non matches");

	}

}
