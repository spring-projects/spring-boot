/*
 * Copyright 2012-present the original author or authors.
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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

/**
 * An {@link ApplicationListener} that logs the delta of condition evaluation across
 * restarts.
 *
 * @author Andy Wilkinson
 */
class ConditionEvaluationDeltaLoggingListener
		implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

	private static final ConcurrentHashMap<String, ConditionEvaluationReport> previousReports = new ConcurrentHashMap<>();

	private static final Log logger = LogFactory.getLog(ConditionEvaluationDeltaLoggingListener.class);

	@SuppressWarnings("NullAway.Init")
	private volatile ApplicationContext context;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (!event.getApplicationContext().equals(this.context)) {
			return;
		}
		ConditionEvaluationReport report = event.getApplicationContext().getBean(ConditionEvaluationReport.class);
		String contextId = event.getApplicationContext().getId();
		ConditionEvaluationReport previousReport = previousReports.get(contextId);
		if (previousReport != null) {
			ConditionEvaluationReport delta = report.getDelta(previousReport);
			if (!delta.getConditionAndOutcomesBySource().isEmpty() || !delta.getExclusions().isEmpty()
					|| !delta.getUnconditionalClasses().isEmpty()) {
				if (logger.isInfoEnabled()) {
					logger.info("Condition evaluation delta:"
							+ new ConditionEvaluationReportMessage(delta, "CONDITION EVALUATION DELTA"));
				}
			}
			else {
				logger.info("Condition evaluation unchanged");
			}
		}
		Assert.state(contextId != null, "'contextId' must not be null");
		previousReports.put(contextId, report);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.context = applicationContext;
	}

}
