/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * An {@link ApplicationListener} that logs the delta of condition evaluation across
 * restarts.
 *
 * @author Andy Wilkinson
 */
class ConditionEvaluationDeltaLoggingListener implements ApplicationListener<ApplicationReadyEvent> {

	private final Log logger = LogFactory.getLog(getClass());

	private static ConditionEvaluationReport previousReport;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		ConditionEvaluationReport report = event.getApplicationContext().getBean(ConditionEvaluationReport.class);
		if (previousReport != null) {
			ConditionEvaluationReport delta = report.getDelta(previousReport);
			if (!delta.getConditionAndOutcomesBySource().isEmpty() || !delta.getExclusions().isEmpty()
					|| !delta.getUnconditionalClasses().isEmpty()) {
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Condition evaluation delta:"
							+ new ConditionEvaluationReportMessage(delta, "CONDITION EVALUATION DELTA"));
				}
			}
			else {
				this.logger.info("Condition evaluation unchanged");
			}
		}
		previousReport = report;
	}

}
