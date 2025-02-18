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

package org.springframework.boot.autoconfigure.logging;

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
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		logConditionEvaluationReport(beanFactory);
		return null;
	}

	private void logConditionEvaluationReport(ConfigurableListableBeanFactory beanFactory) {
		new ConditionEvaluationReportLogger(LogLevel.DEBUG, () -> ConditionEvaluationReport.get(beanFactory))
			.logReport(false);
	}

}
