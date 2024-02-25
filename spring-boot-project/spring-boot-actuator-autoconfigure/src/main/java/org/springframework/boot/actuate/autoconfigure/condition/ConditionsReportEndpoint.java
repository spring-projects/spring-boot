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

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
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

	/**
     * Constructs a new ConditionsReportEndpoint with the specified ConfigurableApplicationContext.
     * 
     * @param context the ConfigurableApplicationContext to be used by the ConditionsReportEndpoint
     */
    public ConditionsReportEndpoint(ConfigurableApplicationContext context) {
		this.context = context;
	}

	/**
     * Returns the conditions descriptor for the current application context.
     * 
     * This method retrieves the conditions descriptor by iterating through the application context hierarchy
     * and collecting the context condition evaluations for each context. The conditions descriptor provides
     * information about the conditions that were evaluated for each context.
     * 
     * @return the conditions descriptor for the current application context
     */
    @ReadOperation
	public ConditionsDescriptor conditions() {
		Map<String, ContextConditionsDescriptor> contextConditionEvaluations = new HashMap<>();
		ConfigurableApplicationContext target = this.context;
		while (target != null) {
			contextConditionEvaluations.put(target.getId(), new ContextConditionsDescriptor(target));
			target = getConfigurableParent(target);
		}
		return new ConditionsDescriptor(contextConditionEvaluations);
	}

	/**
     * Retrieves the configurable parent application context of the given application context.
     * 
     * @param context the application context to retrieve the parent from
     * @return the configurable parent application context, or null if the parent is not a configurable application context
     */
    private ConfigurableApplicationContext getConfigurableParent(ConfigurableApplicationContext context) {
		ApplicationContext parent = context.getParent();
		if (parent instanceof ConfigurableApplicationContext configurableParent) {
			return configurableParent;
		}
		return null;
	}

	/**
	 * A description of an application's condition evaluation.
	 */
	public static final class ConditionsDescriptor implements OperationResponseBody {

		private final Map<String, ContextConditionsDescriptor> contexts;

		/**
         * Constructs a new ConditionsDescriptor with the specified contexts.
         *
         * @param contexts a map of context names to ContextConditionsDescriptor objects
         */
        private ConditionsDescriptor(Map<String, ContextConditionsDescriptor> contexts) {
			this.contexts = contexts;
		}

		/**
         * Returns the map of contexts associated with this ConditionsDescriptor.
         *
         * @return the map of contexts
         */
        public Map<String, ContextConditionsDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * A description of an application context's condition evaluation, primarily intended
	 * for serialization to JSON.
	 */
	@JsonInclude(Include.NON_EMPTY)
	public static final class ContextConditionsDescriptor {

		private final MultiValueMap<String, MessageAndConditionDescriptor> positiveMatches;

		private final Map<String, MessageAndConditionsDescriptor> negativeMatches;

		private final List<String> exclusions;

		private final Set<String> unconditionalClasses;

		private final String parentId;

		/**
         * Constructs a new ContextConditionsDescriptor object with the given ConfigurableApplicationContext.
         * 
         * @param context the ConfigurableApplicationContext to use for constructing the ContextConditionsDescriptor
         */
        public ContextConditionsDescriptor(ConfigurableApplicationContext context) {
			ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
			this.positiveMatches = new LinkedMultiValueMap<>();
			this.negativeMatches = new LinkedHashMap<>();
			this.exclusions = report.getExclusions();
			this.unconditionalClasses = report.getUnconditionalClasses();
			report.getConditionAndOutcomesBySource().forEach(this::add);
			this.parentId = (context.getParent() != null) ? context.getParent().getId() : null;
		}

		/**
         * Adds a source and its corresponding condition and outcomes to the context conditions descriptor.
         * 
         * @param source The source to be added.
         * @param conditionAndOutcomes The condition and outcomes associated with the source.
         */
        private void add(String source, ConditionAndOutcomes conditionAndOutcomes) {
			String name = ClassUtils.getShortName(source);
			if (conditionAndOutcomes.isFullMatch()) {
				conditionAndOutcomes.forEach((conditionAndOutcome) -> this.positiveMatches.add(name,
						new MessageAndConditionDescriptor(conditionAndOutcome)));
			}
			else {
				this.negativeMatches.put(name, new MessageAndConditionsDescriptor(conditionAndOutcomes));
			}
		}

		/**
         * Returns the positive matches stored in a map.
         * 
         * @return a map containing positive matches, where the keys are strings and the values are lists of MessageAndConditionDescriptor objects
         */
        public Map<String, List<MessageAndConditionDescriptor>> getPositiveMatches() {
			return this.positiveMatches;
		}

		/**
         * Returns the map of negative matches.
         * 
         * @return the map of negative matches
         */
        public Map<String, MessageAndConditionsDescriptor> getNegativeMatches() {
			return this.negativeMatches;
		}

		/**
         * Returns the list of exclusions.
         *
         * @return the list of exclusions
         */
        public List<String> getExclusions() {
			return this.exclusions;
		}

		/**
         * Returns the set of unconditional classes.
         * 
         * @return the set of unconditional classes
         */
        public Set<String> getUnconditionalClasses() {
			return this.unconditionalClasses;
		}

		/**
         * Returns the parent ID of the ContextConditionsDescriptor.
         *
         * @return the parent ID of the ContextConditionsDescriptor
         */
        public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Adapts {@link ConditionAndOutcomes} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "notMatched", "matched" })
	public static class MessageAndConditionsDescriptor {

		private final List<MessageAndConditionDescriptor> notMatched = new ArrayList<>();

		private final List<MessageAndConditionDescriptor> matched = new ArrayList<>();

		/**
         * Constructs a new MessageAndConditionsDescriptor object with the given ConditionAndOutcomes.
         * 
         * @param conditionAndOutcomes the ConditionAndOutcomes to be used for constructing the object
         */
        public MessageAndConditionsDescriptor(ConditionAndOutcomes conditionAndOutcomes) {
			for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
				List<MessageAndConditionDescriptor> target = (conditionAndOutcome.getOutcome().isMatch() ? this.matched
						: this.notMatched);
				target.add(new MessageAndConditionDescriptor(conditionAndOutcome));
			}
		}

		/**
         * Returns the list of MessageAndConditionDescriptors that did not match.
         *
         * @return the list of MessageAndConditionDescriptors that did not match
         */
        public List<MessageAndConditionDescriptor> getNotMatched() {
			return this.notMatched;
		}

		/**
         * Returns the list of MessageAndConditionDescriptor objects that have been matched.
         *
         * @return the list of matched MessageAndConditionDescriptor objects
         */
        public List<MessageAndConditionDescriptor> getMatched() {
			return this.matched;
		}

	}

	/**
	 * Adapts {@link ConditionAndOutcome} to a JSON friendly structure.
	 */
	@JsonPropertyOrder({ "condition", "message" })
	public static class MessageAndConditionDescriptor {

		private final String condition;

		private final String message;

		/**
         * Constructs a new MessageAndConditionDescriptor object with the given ConditionAndOutcome.
         * 
         * @param conditionAndOutcome the ConditionAndOutcome object containing the condition and outcome
         */
        public MessageAndConditionDescriptor(ConditionAndOutcome conditionAndOutcome) {
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

		/**
         * Returns the condition of the MessageAndConditionDescriptor.
         *
         * @return the condition of the MessageAndConditionDescriptor
         */
        public String getCondition() {
			return this.condition;
		}

		/**
         * Returns the message associated with this MessageAndConditionDescriptor.
         *
         * @return the message associated with this MessageAndConditionDescriptor
         */
        public String getMessage() {
			return this.message;
		}

	}

}
