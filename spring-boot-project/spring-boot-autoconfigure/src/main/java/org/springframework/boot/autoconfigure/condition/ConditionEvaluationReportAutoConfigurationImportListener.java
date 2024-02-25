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

package org.springframework.boot.autoconfigure.condition;

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

	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * This method is called when an AutoConfigurationImportEvent is triggered. It records
	 * the evaluation candidates and exclusions in the ConditionEvaluationReport.
	 * @param event The AutoConfigurationImportEvent that triggered this method.
	 */
	@Override
	public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
		if (this.beanFactory != null) {
			ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
			report.recordEvaluationCandidates(event.getCandidateConfigurations());
			report.recordExclusions(event.getExclusions());
		}
	}

	/**
	 * Set the BeanFactory that this listener runs in.
	 * <p>
	 * Invoked after population of normal bean properties but before an init callback such
	 * as InitializingBean's {@code afterPropertiesSet} or a custom init-method. Invoked
	 * after ApplicationContextAware's {@code setApplicationContext}.
	 * @param beanFactory the BeanFactory object to be set
	 * @throws BeansException if any error occurs while setting the BeanFactory
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory)
				? listableBeanFactory : null;
	}

}
