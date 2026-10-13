/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.logging;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.logging.LogLevel;

/**
 * {@link BeanFactoryInitializationAotProcessor} that logs the
 * {@link ConditionEvaluationReport} during ahead-of-time processing.
 *
 * @author Andy Wilkinson
 */
class ConditionEvaluationReportLoggingProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
			ConfigurableListableBeanFactory beanFactory) {
		logConditionEvaluationReport(beanFactory);
		return null;
	}

	private void logConditionEvaluationReport(ConfigurableListableBeanFactory beanFactory) {
		new ConditionEvaluationReportLogger(LogLevel.DEBUG, () -> ConditionEvaluationReport.get(beanFactory))
			.logReport(false);
	}

}
