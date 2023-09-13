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

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.Assert;

/**
 * Logs the {@link ConditionEvaluationReport}.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class ConditionEvaluationReportLogger {

	private final Log logger = LogFactory.getLog(getClass());

	private final Supplier<ConditionEvaluationReport> reportSupplier;

	private final LogLevel logLevel;

	ConditionEvaluationReportLogger(LogLevel logLevel, Supplier<ConditionEvaluationReport> reportSupplier) {
		Assert.isTrue(isInfoOrDebug(logLevel), "LogLevel must be INFO or DEBUG");
		this.logLevel = logLevel;
		this.reportSupplier = reportSupplier;
	}

	private boolean isInfoOrDebug(LogLevel logLevelForReport) {
		return LogLevel.INFO.equals(logLevelForReport) || LogLevel.DEBUG.equals(logLevelForReport);
	}

	void logReport(boolean isCrashReport) {
		ConditionEvaluationReport report = this.reportSupplier.get();
		if (report == null) {
			this.logger.info("Unable to provide the condition evaluation report");
			return;
		}
		if (!report.getConditionAndOutcomesBySource().isEmpty()) {
			if (this.logLevel.equals(LogLevel.INFO)) {
				if (this.logger.isInfoEnabled()) {
					this.logger.info(new ConditionEvaluationReportMessage(report));
				}
				else if (isCrashReport) {
					logMessage("info");
				}
			}
			else {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug(new ConditionEvaluationReportMessage(report));
				}
				else if (isCrashReport) {
					logMessage("debug");
				}
			}
		}
	}

	private void logMessage(String logLevel) {
		this.logger.info(String.format("%n%nError starting ApplicationContext. To display the "
				+ "condition evaluation report re-run your application with '%s' enabled.", logLevel));
	}

}
