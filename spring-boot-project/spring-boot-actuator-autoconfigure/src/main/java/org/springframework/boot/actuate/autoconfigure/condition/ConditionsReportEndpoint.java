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

package org.springframework.boot.actuate.autoconfigure.condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint @Endpoint} to expose the {@link ConditionEvaluationReport}.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "conditions")
public class ConditionsReportEndpoint {

	private final ConfigurableApplicationContext context;

	public ConditionsReportEndpoint(ConfigurableApplicationContext context) {
		this.context = context;
	}

	@ReadOperation
	public ApplicationConditionEvaluation applicationConditionEvaluation() {
		Map<String, ContextConditionEvaluation> contextConditionEvaluations = new HashMap<>();
		ConfigurableApplicationContext target = this.context;
		while (target != null) {
			contextConditionEvaluations.put(target.getId(), new ContextConditionEvaluation(target));
			target = getConfigurableParent(target);
		}
		return new ApplicationConditionEvaluation(contextConditionEvaluations);
	}

	private ConfigurableApplicationContext getConfigurableParent(ConfigurableApplicationContext context) {
		ApplicationContext parent = context.getParent();
		if (parent instanceof ConfigurableApplicationContext configurableParent) {
			return configurableParent;
		}
		return null;
	}

	/**
	 * A description of an application's condition evaluation, primarily intended for
	 * serialization to JSON.
	 */
	public static final class ApplicationConditionEvaluation {

		private final Map<String, ContextConditionEvaluation> contexts;

		private ApplicationConditionEvaluation(Map<String, ContextConditionEvaluation> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextConditionEvaluation> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * A description of an application context's condition evaluation, primarily intended
	 * for serialization to JSON.
	 */
	@JsonInclude(Include.NON_EMPTY)
	public static final class ContextConditionEvaluation {

		private final MultiValueMap<String, MessageAndCondition> positiveMatches;

		private final Map<String, MessageAndConditions> negativeMatches;

		private final List<String> exclusions;

		private final Set<String> unconditionalClasses;

		private final String parentId;

		public ContextConditionEvaluation(ConfigurableApplicationContext context) {
			ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
			this.positiveMatches = new LinkedMultiValueMap<>();
			this.negativeMatches = new LinkedHashMap<>();
			this.exclusions = report.getExclusions();
			this.unconditionalClasses = report.getUnconditionalClasses();
			report.getConditionAndOutcomesBySource().forEach(this::add);
			this.parentId = (context.getParent() != null) ? context.getParent().getId() : null;
		}

		private void add(String source, ConditionAndOutcomes conditionAndOutcomes) {
			String name = ClassUtils.getShortName(source);
			if (conditionAndOutcomes.isFullMatch()) {
				conditionAndOutcomes.forEach((conditionAndOutcome) -> this.positiveMatches.add(name,
						new MessageAndCondition(conditionAndOutcome)));
			}
			else {
				this.negativeMatches.put(name, new MessageAndConditions(conditionAndOutcomes));
			}
		}

		public Map<String, List<MessageAndCondition>> getPositiveMatches() {
			return this.positiveMatches;
		}

		public Map<String, MessageAndConditions> getNegativeMatches() {
			return this.negativeMatches;
		}

		public List<String> getExclusions() {
			return this.exclusions;
		}

		public Set<String> getUnconditionalClasses() {
			return this.unconditionalClasses;
		}

		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Adapts {@link ConditionAndOutcomes} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "notMatched", "matched" })
	public static class MessageAndConditions {

		private final List<MessageAndCondition> notMatched = new ArrayList<>();

		private final List<MessageAndCondition> matched = new ArrayList<>();

		public MessageAndConditions(ConditionAndOutcomes conditionAndOutcomes) {
			for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
				List<MessageAndCondition> target = (conditionAndOutcome.getOutcome().isMatch() ? this.matched
						: this.notMatched);
				target.add(new MessageAndCondition(conditionAndOutcome));
			}
		}

		public List<MessageAndCondition> getNotMatched() {
			return this.notMatched;
		}

		public List<MessageAndCondition> getMatched() {
			return this.matched;
		}

	}

	/**
	 * Adapts {@link ConditionAndOutcome} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "condition", "message" })
	public static class MessageAndCondition {

		private final String condition;

		private final String message;

		public MessageAndCondition(ConditionAndOutcome conditionAndOutcome) {
			Condition condition = conditionAndOutcome.getCondition();
			ConditionOutcome outcome = conditionAndOutcome.getOutcome();
			this.condition = ClassUtils.getShortName(condition.getClass());
			if (StringUtils.hasLength(outcome.getMessage())) {
				this.message = outcome.getMessage();
			}
			else {
				this.message = outcome.isMatch() ? "matched" : "did not match";
			}
		}

		public String getCondition() {
			return this.condition;
		}

		public String getMessage() {
			return this.message;
		}

	}

}
