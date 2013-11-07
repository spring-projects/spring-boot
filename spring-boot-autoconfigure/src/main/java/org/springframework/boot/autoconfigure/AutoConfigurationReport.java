/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.Condition;

/**
 * Records auto-configuration details for reporting and logging.
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 */
public class AutoConfigurationReport {

	private static final String BEAN_NAME = "autoConfigurationReport";

	private final SortedMap<String, ConditionAndOutcomes> outcomes = new TreeMap<String, ConditionAndOutcomes>();

	/**
	 * Private constructor.
	 * @see #get(ConfigurableListableBeanFactory)
	 */
	private AutoConfigurationReport() {
	}

	/**
	 * Record the occurrence of condition evaluation.
	 * @param source the source of the condition (class or method name)
	 * @param condition the condition evaluated
	 * @param outcome the condition outcome
	 */
	public void recordConditionEvaluation(String source, Condition condition,
			ConditionOutcome outcome) {
		if (!this.outcomes.containsKey(source)) {
			this.outcomes.put(source, new ConditionAndOutcomes());
		}
		this.outcomes.get(source).add(condition, outcome);
	}

	/**
	 * Returns condition outcomes from this report, grouped by the source.
	 */
	public Map<String, ConditionAndOutcomes> getConditionAndOutcomesBySource() {
		return Collections.unmodifiableMap(this.outcomes);
	}

	/**
	 * Obtain a {@link AutoConfigurationReport} for the specified bean factory.
	 * @param beanFactory the bean factory
	 * @return an existing or new {@link AutoConfigurationReport}
	 */
	public static AutoConfigurationReport get(ConfigurableListableBeanFactory beanFactory) {
		synchronized (beanFactory) {
			try {
				return beanFactory.getBean(BEAN_NAME, AutoConfigurationReport.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				AutoConfigurationReport report = new AutoConfigurationReport();
				beanFactory.registerSingleton(BEAN_NAME, report);
				return report;
			}
		}
	}

	/**
	 * Provides access to a number of {@link ConditionAndOutcome} items.
	 */
	public static class ConditionAndOutcomes implements Iterable<ConditionAndOutcome> {

		private List<ConditionAndOutcome> outcomes = new ArrayList<ConditionAndOutcome>();

		public void add(Condition condition, ConditionOutcome outcome) {
			this.outcomes.add(new ConditionAndOutcome(condition, outcome));
		}

		/**
		 * Return {@code true} if all outcomes match.
		 */
		public boolean isFullMatch() {
			for (ConditionAndOutcome conditionAndOutcomes : this) {
				if (!conditionAndOutcomes.getOutcome().isMatch()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Iterator<ConditionAndOutcome> iterator() {
			return Collections.unmodifiableList(this.outcomes).iterator();
		}

	}

	/**
	 * Provides access to a single {@link Condition} and {@link ConditionOutcome}.
	 */
	public static class ConditionAndOutcome {

		private final Condition condition;

		private final ConditionOutcome outcome;

		public ConditionAndOutcome(Condition condition, ConditionOutcome outcome) {
			this.condition = condition;
			this.outcome = outcome;
		}

		public Condition getCondition() {
			return this.condition;
		}

		public ConditionOutcome getOutcome() {
			return this.outcome;
		}
	}

}
