package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.List;

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
