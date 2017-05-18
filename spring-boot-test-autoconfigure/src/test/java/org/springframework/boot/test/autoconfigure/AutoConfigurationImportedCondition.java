/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure;

import org.assertj.core.api.Condition;
import org.assertj.core.description.TextDescription;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.context.ApplicationContext;

/**
 * AssertJ {@link Condition} that checks that an auto-configuration has been imported.
 *
 * @author Andy Wilkinson
 */
public final class AutoConfigurationImportedCondition
		extends Condition<ApplicationContext> {

	private final Class<?> autoConfigurationClass;

	private AutoConfigurationImportedCondition(Class<?> autoConfigurationClass) {
		super(new TextDescription("%s imported", autoConfigurationClass.getName()));
		this.autoConfigurationClass = autoConfigurationClass;
	}

	@Override
	public boolean matches(ApplicationContext context) {
		ConditionEvaluationReport report = ConditionEvaluationReport
				.get((ConfigurableListableBeanFactory) context
						.getAutowireCapableBeanFactory());
		return report.getConditionAndOutcomesBySource().keySet()
				.contains(this.autoConfigurationClass.getName());
	}

	/**
	 * Returns a {@link Condition} that verifies that the given
	 * {@code autoConfigurationClass} has been imported.
	 * @param autoConfigurationClass the auto-configuration class
	 * @return the condition
	 */
	public static AutoConfigurationImportedCondition importedAutoConfiguration(
			Class<?> autoConfigurationClass) {
		return new AutoConfigurationImportedCondition(autoConfigurationClass);
	}

}
