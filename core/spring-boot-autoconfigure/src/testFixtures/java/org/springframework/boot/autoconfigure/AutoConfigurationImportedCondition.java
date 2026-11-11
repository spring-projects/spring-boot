/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

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
public final class AutoConfigurationImportedCondition extends Condition<ApplicationContext> {

	private final Class<?> autoConfigurationClass;

	private AutoConfigurationImportedCondition(Class<?> autoConfigurationClass) {
		super(new TextDescription("%s imported", autoConfigurationClass.getName()));
		this.autoConfigurationClass = autoConfigurationClass;
	}

	@Override
	public boolean matches(ApplicationContext context) {
		ConditionEvaluationReport report = ConditionEvaluationReport
			.get((ConfigurableListableBeanFactory) context.getAutowireCapableBeanFactory());
		return report.getConditionAndOutcomesBySource().containsKey(this.autoConfigurationClass.getName())
				|| report.getUnconditionalClasses().contains(this.autoConfigurationClass.getName());
	}

	/**
	 * Returns a {@link Condition} that verifies that the given
	 * {@code autoConfigurationClass} has been imported.
	 * @param autoConfigurationClass the auto-configuration class
	 * @return the condition
	 */
	public static AutoConfigurationImportedCondition importedAutoConfiguration(Class<?> autoConfigurationClass) {
		return new AutoConfigurationImportedCondition(autoConfigurationClass);
	}

}
