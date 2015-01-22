package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Condition;

/**
 * {@link Condition} that will match when none of the nested class conditions match.
 * Can be used to create composite conditions, for example:
 *
 * <pre class="code">
 * static class OnJndiOrProperty extends NoneOfNestedConditions {
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
public abstract class NoneOfNestedConditions extends AbstractNestedCondition {

	public NoneOfNestedConditions(ConfigurationPhase configurationPhase) {
		super(configurationPhase);
	}

	@Override
	protected ConditionOutcome buildConditionOutcome(List<ConditionOutcome> outcomes) {
		List<ConditionOutcome> match = new ArrayList<ConditionOutcome>();
		List<ConditionOutcome> nonMatch = new ArrayList<ConditionOutcome>();
		for (ConditionOutcome outcome : outcomes) {
			if (outcome.isMatch()) {
				match.add(outcome);
			} else {
				nonMatch.add(outcome);
			}
		}
		return new ConditionOutcome(match.size() == 0, "none of match resulted in " + match + " matches and "
				+ nonMatch + " non matches");
	}

}
