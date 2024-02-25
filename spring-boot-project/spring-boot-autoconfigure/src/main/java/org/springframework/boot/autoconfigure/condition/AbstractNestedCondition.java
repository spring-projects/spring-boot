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

package org.springframework.boot.autoconfigure.condition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Abstract base class for nested conditions.
 *
 * @author Phillip Webb
 * @since 1.5.22
 */
public abstract class AbstractNestedCondition extends SpringBootCondition implements ConfigurationCondition {

	private final ConfigurationPhase configurationPhase;

	/**
     * Constructs a new instance of AbstractNestedCondition with the specified configuration phase.
     *
     * @param configurationPhase the configuration phase to be set (must not be null)
     * @throws IllegalArgumentException if the configuration phase is null
     */
    AbstractNestedCondition(ConfigurationPhase configurationPhase) {
		Assert.notNull(configurationPhase, "ConfigurationPhase must not be null");
		this.configurationPhase = configurationPhase;
	}

	/**
     * Returns the configuration phase of this AbstractNestedCondition.
     *
     * @return the configuration phase of this AbstractNestedCondition
     */
    @Override
	public ConfigurationPhase getConfigurationPhase() {
		return this.configurationPhase;
	}

	/**
     * Determines the match outcome for the given condition context and annotated type metadata.
     * 
     * @param context the condition context
     * @param metadata the annotated type metadata
     * @return the match outcome for the condition
     */
    @Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String className = getClass().getName();
		MemberConditions memberConditions = new MemberConditions(context, this.configurationPhase, className);
		MemberMatchOutcomes memberOutcomes = new MemberMatchOutcomes(memberConditions);
		return getFinalMatchOutcome(memberOutcomes);
	}

	/**
     * Returns the final outcome of a member match based on the given member outcomes.
     *
     * @param memberOutcomes the member match outcomes to evaluate
     * @return the final outcome of the member match
     */
    protected abstract ConditionOutcome getFinalMatchOutcome(MemberMatchOutcomes memberOutcomes);

	/**
     * MemberMatchOutcomes class.
     */
    protected static class MemberMatchOutcomes {

		private final List<ConditionOutcome> all;

		private final List<ConditionOutcome> matches;

		private final List<ConditionOutcome> nonMatches;

		/**
         * Creates a new instance of MemberMatchOutcomes with the given MemberConditions.
         * 
         * @param memberConditions the MemberConditions object containing the match outcomes
         */
        public MemberMatchOutcomes(MemberConditions memberConditions) {
			this.all = Collections.unmodifiableList(memberConditions.getMatchOutcomes());
			List<ConditionOutcome> matches = new ArrayList<>();
			List<ConditionOutcome> nonMatches = new ArrayList<>();
			for (ConditionOutcome outcome : this.all) {
				(outcome.isMatch() ? matches : nonMatches).add(outcome);
			}
			this.matches = Collections.unmodifiableList(matches);
			this.nonMatches = Collections.unmodifiableList(nonMatches);
		}

		/**
         * Retrieves all the condition outcomes.
         * 
         * @return a list of ConditionOutcome objects representing all the condition outcomes
         */
        public List<ConditionOutcome> getAll() {
			return this.all;
		}

		/**
         * Returns the list of ConditionOutcomes representing the matches.
         *
         * @return the list of ConditionOutcomes representing the matches
         */
        public List<ConditionOutcome> getMatches() {
			return this.matches;
		}

		/**
         * Returns a list of ConditionOutcomes that did not match.
         *
         * @return a list of ConditionOutcomes that did not match
         */
        public List<ConditionOutcome> getNonMatches() {
			return this.nonMatches;
		}

	}

	/**
     * MemberConditions class.
     */
    private static class MemberConditions {

		private final ConditionContext context;

		private final MetadataReaderFactory readerFactory;

		private final Map<AnnotationMetadata, List<Condition>> memberConditions;

		/**
         * Initializes a new instance of the MemberConditions class.
         * 
         * @param context the ConditionContext object representing the context in which the conditions are evaluated
         * @param phase the ConfigurationPhase object representing the phase in which the conditions are evaluated
         * @param className the name of the class for which the member conditions are being evaluated
         */
        MemberConditions(ConditionContext context, ConfigurationPhase phase, String className) {
			this.context = context;
			this.readerFactory = new SimpleMetadataReaderFactory(context.getResourceLoader());
			String[] members = getMetadata(className).getMemberClassNames();
			this.memberConditions = getMemberConditions(members, phase, className);
		}

		/**
         * Retrieves the member conditions for a given set of members, configuration phase, and class name.
         * 
         * @param members the array of member names
         * @param phase the configuration phase
         * @param className the name of the class
         * @return a map containing the annotation metadata and corresponding list of conditions for each member
         */
        private Map<AnnotationMetadata, List<Condition>> getMemberConditions(String[] members, ConfigurationPhase phase,
				String className) {
			MultiValueMap<AnnotationMetadata, Condition> memberConditions = new LinkedMultiValueMap<>();
			for (String member : members) {
				AnnotationMetadata metadata = getMetadata(member);
				for (String[] conditionClasses : getConditionClasses(metadata)) {
					for (String conditionClass : conditionClasses) {
						Condition condition = getCondition(conditionClass);
						validateMemberCondition(condition, phase, className);
						memberConditions.add(metadata, condition);
					}
				}
			}
			return Collections.unmodifiableMap(memberConditions);
		}

		/**
         * Validates the member condition based on the given condition, nested phase, and nested class name.
         * 
         * @param condition         The condition to be validated.
         * @param nestedPhase       The nested phase to be checked.
         * @param nestedClassName   The name of the nested class.
         * @throws IllegalStateException if the nested condition uses a configuration phase that is inappropriate for the given condition.
         */
        private void validateMemberCondition(Condition condition, ConfigurationPhase nestedPhase,
				String nestedClassName) {
			if (nestedPhase == ConfigurationPhase.PARSE_CONFIGURATION
					&& condition instanceof ConfigurationCondition configurationCondition) {
				ConfigurationPhase memberPhase = configurationCondition.getConfigurationPhase();
				if (memberPhase == ConfigurationPhase.REGISTER_BEAN) {
					throw new IllegalStateException("Nested condition " + nestedClassName + " uses a configuration "
							+ "phase that is inappropriate for " + condition.getClass());
				}
			}
		}

		/**
         * Retrieves the annotation metadata for a given class name.
         * 
         * @param className the name of the class to retrieve the metadata for
         * @return the annotation metadata for the specified class
         * @throws IllegalStateException if an error occurs while retrieving the metadata
         */
        private AnnotationMetadata getMetadata(String className) {
			try {
				return this.readerFactory.getMetadataReader(className).getAnnotationMetadata();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
         * Retrieves the condition classes from the given metadata.
         * 
         * @param metadata the annotated type metadata
         * @return a list of condition classes as string arrays
         */
        @SuppressWarnings("unchecked")
		private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
			MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName(),
					true);
			Object values = (attributes != null) ? attributes.get("value") : null;
			return (List<String[]>) ((values != null) ? values : Collections.emptyList());
		}

		/**
         * Retrieves the condition object based on the provided condition class name.
         * 
         * @param conditionClassName the fully qualified name of the condition class
         * @return the condition object
         * @throws IllegalArgumentException if the condition class cannot be resolved or instantiated
         */
        private Condition getCondition(String conditionClassName) {
			Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName, this.context.getClassLoader());
			return (Condition) BeanUtils.instantiateClass(conditionClass);
		}

		/**
         * Returns a list of ConditionOutcomes for the match.
         * 
         * @return the list of ConditionOutcomes for the match
         */
        List<ConditionOutcome> getMatchOutcomes() {
			List<ConditionOutcome> outcomes = new ArrayList<>();
			this.memberConditions.forEach((metadata, conditions) -> outcomes
				.add(new MemberOutcomes(this.context, metadata, conditions).getUltimateOutcome()));
			return Collections.unmodifiableList(outcomes);
		}

	}

	/**
     * MemberOutcomes class.
     */
    private static class MemberOutcomes {

		private final ConditionContext context;

		private final AnnotationMetadata metadata;

		private final List<ConditionOutcome> outcomes;

		/**
         * Constructs a new instance of MemberOutcomes with the given ConditionContext, AnnotationMetadata, and List of Conditions.
         * 
         * @param context the ConditionContext used for evaluating conditions
         * @param metadata the AnnotationMetadata used for evaluating conditions
         * @param conditions the List of Conditions to evaluate
         */
        MemberOutcomes(ConditionContext context, AnnotationMetadata metadata, List<Condition> conditions) {
			this.context = context;
			this.metadata = metadata;
			this.outcomes = new ArrayList<>(conditions.size());
			for (Condition condition : conditions) {
				this.outcomes.add(getConditionOutcome(metadata, condition));
			}
		}

		/**
         * Returns the condition outcome for the given metadata and condition.
         * 
         * @param metadata the annotation metadata
         * @param condition the condition to evaluate
         * @return the condition outcome
         */
        private ConditionOutcome getConditionOutcome(AnnotationMetadata metadata, Condition condition) {
			if (condition instanceof SpringBootCondition springBootCondition) {
				return springBootCondition.getMatchOutcome(this.context, metadata);
			}
			return new ConditionOutcome(condition.matches(this.context, metadata), ConditionMessage.empty());
		}

		/**
         * Returns the ultimate outcome of the nested conditions.
         * 
         * @return The ultimate outcome of the nested conditions.
         */
        ConditionOutcome getUltimateOutcome() {
			ConditionMessage.Builder message = ConditionMessage
				.forCondition("NestedCondition on " + ClassUtils.getShortName(this.metadata.getClassName()));
			if (this.outcomes.size() == 1) {
				ConditionOutcome outcome = this.outcomes.get(0);
				return new ConditionOutcome(outcome.isMatch(), message.because(outcome.getMessage()));
			}
			List<ConditionOutcome> match = new ArrayList<>();
			List<ConditionOutcome> nonMatch = new ArrayList<>();
			for (ConditionOutcome outcome : this.outcomes) {
				(outcome.isMatch() ? match : nonMatch).add(outcome);
			}
			if (nonMatch.isEmpty()) {
				return ConditionOutcome.match(message.found("matching nested conditions").items(match));
			}
			return ConditionOutcome.noMatch(message.found("non-matching nested conditions").items(nonMatch));
		}

	}

}
