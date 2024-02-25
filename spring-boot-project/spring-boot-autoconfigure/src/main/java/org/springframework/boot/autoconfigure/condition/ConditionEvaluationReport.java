/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Records condition evaluation details for reporting and logging.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.0.0
 */
public final class ConditionEvaluationReport {

	private static final String BEAN_NAME = "autoConfigurationReport";

	private static final AncestorsMatchedCondition ANCESTOR_CONDITION = new AncestorsMatchedCondition();

	private final SortedMap<String, ConditionAndOutcomes> outcomes = new TreeMap<>();

	private boolean addedAncestorOutcomes;

	private ConditionEvaluationReport parent;

	private final List<String> exclusions = new ArrayList<>();

	private final Set<String> unconditionalClasses = new HashSet<>();

	/**
	 * Private constructor.
	 * @see #get(ConfigurableListableBeanFactory)
	 */
	private ConditionEvaluationReport() {
	}

	/**
	 * Record the occurrence of condition evaluation.
	 * @param source the source of the condition (class or method name)
	 * @param condition the condition evaluated
	 * @param outcome the condition outcome
	 */
	public void recordConditionEvaluation(String source, Condition condition, ConditionOutcome outcome) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(condition, "Condition must not be null");
		Assert.notNull(outcome, "Outcome must not be null");
		this.unconditionalClasses.remove(source);
		this.outcomes.computeIfAbsent(source, (key) -> new ConditionAndOutcomes()).add(condition, outcome);
		this.addedAncestorOutcomes = false;
	}

	/**
	 * Records the names of the classes that have been excluded from condition evaluation.
	 * @param exclusions the names of the excluded classes
	 */
	public void recordExclusions(Collection<String> exclusions) {
		Assert.notNull(exclusions, "exclusions must not be null");
		this.exclusions.addAll(exclusions);
	}

	/**
	 * Records the names of the classes that are candidates for condition evaluation.
	 * @param evaluationCandidates the names of the classes whose conditions will be
	 * evaluated
	 */
	public void recordEvaluationCandidates(List<String> evaluationCandidates) {
		Assert.notNull(evaluationCandidates, "evaluationCandidates must not be null");
		this.unconditionalClasses.addAll(evaluationCandidates);
	}

	/**
	 * Returns condition outcomes from this report, grouped by the source.
	 * @return the condition outcomes
	 */
	public Map<String, ConditionAndOutcomes> getConditionAndOutcomesBySource() {
		if (!this.addedAncestorOutcomes) {
			this.outcomes.forEach((source, sourceOutcomes) -> {
				if (!sourceOutcomes.isFullMatch()) {
					addNoMatchOutcomeToAncestors(source);
				}
			});
			this.addedAncestorOutcomes = true;
		}
		return Collections.unmodifiableMap(this.outcomes);
	}

	/**
	 * Adds a no match outcome to the ancestors of the given source.
	 * @param source the source to add the no match outcome to its ancestors
	 */
	private void addNoMatchOutcomeToAncestors(String source) {
		String prefix = source + "$";
		this.outcomes.forEach((candidateSource, sourceOutcomes) -> {
			if (candidateSource.startsWith(prefix)) {
				ConditionOutcome outcome = ConditionOutcome
					.noMatch(ConditionMessage.forCondition("Ancestor " + source).because("did not match"));
				sourceOutcomes.add(ANCESTOR_CONDITION, outcome);
			}
		});
	}

	/**
	 * Returns the names of the classes that have been excluded from condition evaluation.
	 * @return the names of the excluded classes
	 */
	public List<String> getExclusions() {
		return Collections.unmodifiableList(this.exclusions);
	}

	/**
	 * Returns the names of the classes that were evaluated but were not conditional.
	 * @return the names of the unconditional classes
	 */
	public Set<String> getUnconditionalClasses() {
		Set<String> filtered = new HashSet<>(this.unconditionalClasses);
		this.exclusions.forEach(filtered::remove);
		return Collections.unmodifiableSet(filtered);
	}

	/**
	 * The parent report (from a parent BeanFactory if there is one).
	 * @return the parent report (or null if there isn't one)
	 */
	public ConditionEvaluationReport getParent() {
		return this.parent;
	}

	/**
	 * Attempt to find the {@link ConditionEvaluationReport} for the specified bean
	 * factory.
	 * @param beanFactory the bean factory (may be {@code null})
	 * @return the {@link ConditionEvaluationReport} or {@code null}
	 */
	public static ConditionEvaluationReport find(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			return ConditionEvaluationReport.get((ConfigurableListableBeanFactory) beanFactory);
		}
		return null;
	}

	/**
	 * Obtain a {@link ConditionEvaluationReport} for the specified bean factory.
	 * @param beanFactory the bean factory
	 * @return an existing or new {@link ConditionEvaluationReport}
	 */
	public static ConditionEvaluationReport get(ConfigurableListableBeanFactory beanFactory) {
		synchronized (beanFactory) {
			ConditionEvaluationReport report;
			if (beanFactory.containsSingleton(BEAN_NAME)) {
				report = beanFactory.getBean(BEAN_NAME, ConditionEvaluationReport.class);
			}
			else {
				report = new ConditionEvaluationReport();
				beanFactory.registerSingleton(BEAN_NAME, report);
			}
			locateParent(beanFactory.getParentBeanFactory(), report);
			return report;
		}
	}

	/**
	 * Locates the parent bean factory in the given bean factory and sets it in the
	 * condition evaluation report.
	 * @param beanFactory the bean factory to search for the parent bean factory
	 * @param report the condition evaluation report to set the parent bean factory in
	 */
	private static void locateParent(BeanFactory beanFactory, ConditionEvaluationReport report) {
		if (beanFactory != null && report.parent == null && beanFactory.containsBean(BEAN_NAME)) {
			report.parent = beanFactory.getBean(BEAN_NAME, ConditionEvaluationReport.class);
		}
	}

	/**
	 * Returns the delta between the current ConditionEvaluationReport and the previous
	 * ConditionEvaluationReport. The delta includes any changes in outcomes, exclusions,
	 * and unconditional classes.
	 * @param previousReport The previous ConditionEvaluationReport to compare against.
	 * @return The delta ConditionEvaluationReport.
	 */
	public ConditionEvaluationReport getDelta(ConditionEvaluationReport previousReport) {
		ConditionEvaluationReport delta = new ConditionEvaluationReport();
		this.outcomes.forEach((source, sourceOutcomes) -> {
			ConditionAndOutcomes previous = previousReport.outcomes.get(source);
			if (previous == null || previous.isFullMatch() != sourceOutcomes.isFullMatch()) {
				sourceOutcomes.forEach((conditionAndOutcome) -> delta.recordConditionEvaluation(source,
						conditionAndOutcome.getCondition(), conditionAndOutcome.getOutcome()));
			}
		});
		List<String> newExclusions = new ArrayList<>(this.exclusions);
		newExclusions.removeAll(previousReport.getExclusions());
		delta.recordExclusions(newExclusions);
		List<String> newUnconditionalClasses = new ArrayList<>(this.unconditionalClasses);
		newUnconditionalClasses.removeAll(previousReport.unconditionalClasses);
		delta.unconditionalClasses.addAll(newUnconditionalClasses);
		return delta;
	}

	/**
	 * Provides access to a number of {@link ConditionAndOutcome} items.
	 */
	public static class ConditionAndOutcomes implements Iterable<ConditionAndOutcome> {

		private final Set<ConditionAndOutcome> outcomes = new LinkedHashSet<>();

		/**
		 * Adds a new condition and outcome pair to the list of outcomes.
		 * @param condition the condition to be evaluated
		 * @param outcome the outcome associated with the condition
		 */
		public void add(Condition condition, ConditionOutcome outcome) {
			this.outcomes.add(new ConditionAndOutcome(condition, outcome));
		}

		/**
		 * Return {@code true} if all outcomes match.
		 * @return {@code true} if a full match
		 */
		public boolean isFullMatch() {
			for (ConditionAndOutcome conditionAndOutcomes : this) {
				if (!conditionAndOutcomes.getOutcome().isMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Returns an iterator over the elements in this set of ConditionAndOutcome
		 * objects.
		 * @return an iterator over the elements in this set of ConditionAndOutcome
		 * objects
		 */
		@Override
		public Iterator<ConditionAndOutcome> iterator() {
			return Collections.unmodifiableSet(this.outcomes).iterator();
		}

	}

	/**
	 * Provides access to a single {@link Condition} and {@link ConditionOutcome}.
	 */
	public static class ConditionAndOutcome {

		private final Condition condition;

		private final ConditionOutcome outcome;

		/**
		 * Constructs a new ConditionAndOutcome object with the specified condition and
		 * outcome.
		 * @param condition the condition to be associated with the object
		 * @param outcome the outcome to be associated with the object
		 */
		public ConditionAndOutcome(Condition condition, ConditionOutcome outcome) {
			this.condition = condition;
			this.outcome = outcome;
		}

		/**
		 * Returns the condition of the object.
		 * @return the condition of the object
		 */
		public Condition getCondition() {
			return this.condition;
		}

		/**
		 * Returns the outcome of the condition.
		 * @return the outcome of the condition
		 */
		public ConditionOutcome getOutcome() {
			return this.outcome;
		}

		/**
		 * Compares this ConditionAndOutcome object to the specified object for equality.
		 * @param obj the object to compare to
		 * @return true if the objects are equal, false otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ConditionAndOutcome other = (ConditionAndOutcome) obj;
			return (ObjectUtils.nullSafeEquals(this.condition.getClass(), other.condition.getClass())
					&& ObjectUtils.nullSafeEquals(this.outcome, other.outcome));
		}

		/**
		 * Returns the hash code value for this ConditionAndOutcome object. The hash code
		 * is calculated based on the class of the condition and the outcome.
		 * @return the hash code value for this ConditionAndOutcome object
		 */
		@Override
		public int hashCode() {
			return this.condition.getClass().hashCode() * 31 + this.outcome.hashCode();
		}

		/**
		 * Returns a string representation of the ConditionAndOutcome object.
		 * @return the string representation of the ConditionAndOutcome object, which
		 * includes the class of the condition and the outcome
		 */
		@Override
		public String toString() {
			return this.condition.getClass() + " " + this.outcome;
		}

	}

	/**
	 * AncestorsMatchedCondition class.
	 */
	private static final class AncestorsMatchedCondition implements Condition {

		/**
		 * Determines if the given condition matches the specified context and annotated
		 * type metadata.
		 * @param context the condition context
		 * @param metadata the annotated type metadata
		 * @return {@code true} if the condition matches, {@code false} otherwise
		 * @throws UnsupportedOperationException if the method is not supported
		 */
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			throw new UnsupportedOperationException();
		}

	}

}
