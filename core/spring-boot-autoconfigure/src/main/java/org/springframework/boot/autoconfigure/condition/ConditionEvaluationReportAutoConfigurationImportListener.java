/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportEvent;
import org.springframework.boot.autoconfigure.AutoConfigurationImportListener;

/**
 * {@link AutoConfigurationImportListener} to record results with the
 * {@link ConditionEvaluationReport}.
 *
 * @author Phillip Webb
 */
class ConditionEvaluationReportAutoConfigurationImportListener
		implements AutoConfigurationImportListener, BeanFactoryAware {

	private @Nullable ConfigurableListableBeanFactory beanFactory;

	@Override
	public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
		if (this.beanFactory != null) {
			ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
			report.recordEvaluationCandidates(event.getCandidateConfigurations());
			report.recordExclusions(event.getExclusions());
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory)
				? listableBeanFactory : null;
	}

}
