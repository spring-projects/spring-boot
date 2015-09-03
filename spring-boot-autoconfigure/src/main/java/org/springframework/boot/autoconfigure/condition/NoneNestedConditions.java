package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Condition;

/**
 * {@link Condition} that will match when none of the nested class conditions match. Can
 * be used to create composite conditions, for example:
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
 * @since 1.3.0
 */
public abstract class NoneNestedConditions extends AbstractNestedCondition {

	public NoneNestedConditions(ConfigurationPhase configurationPhase) {
		super(configurationPhase);
	}

	@Override
	protected ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes) {
		return new ConditionOutcome(memberOutcomes.getMatches().isEmpty(),
				"nested none match resulted in " + memberOutcomes.getMatches()
						+ " matches and " + memberOutcomes.getNonMatches()
						+ " non matches");
	}

}
