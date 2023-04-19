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

package org.springframework.boot.autoconfigure.logging;

import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * {@link ApplicationContextInitializer} that writes the {@link ConditionEvaluationReport}
 * to the log. Reports are logged at the {@link LogLevel#DEBUG DEBUG} level. A crash
 * report triggers an info output suggesting the user runs again with debug enabled to
 * display the report.
 * <p>
 * This initializer is not intended to be shared across multiple application context
 * instances.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ConditionEvaluationReportLoggingListener
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private final LogLevel logLevelForReport;

	public ConditionEvaluationReportLoggingListener() {
		this(LogLevel.DEBUG);
	}

	private ConditionEvaluationReportLoggingListener(LogLevel logLevelForReport) {
		Assert.isTrue(isInfoOrDebug(logLevelForReport), "LogLevel must be INFO or DEBUG");
		this.logLevelForReport = logLevelForReport;
	}

	private boolean isInfoOrDebug(LogLevel logLevelForReport) {
		return LogLevel.INFO.equals(logLevelForReport) || LogLevel.DEBUG.equals(logLevelForReport);
	}

	/**
	 * Static factory method that creates a
	 * {@link ConditionEvaluationReportLoggingListener} which logs the report at the
	 * specified log level.
	 * @param logLevelForReport the log level to log the report at
	 * @return a {@link ConditionEvaluationReportLoggingListener} instance.
	 * @since 3.0.0
	 */
	public static ConditionEvaluationReportLoggingListener forLogLevel(LogLevel logLevelForReport) {
		return new ConditionEvaluationReportLoggingListener(logLevelForReport);
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(new ConditionEvaluationReportListener(applicationContext));
	}

	private final class ConditionEvaluationReportListener implements GenericApplicationListener {

		private final ConfigurableApplicationContext context;

		private final ConditionEvaluationReportLogger logger;

		private ConditionEvaluationReportListener(ConfigurableApplicationContext context) {
			this.context = context;
			Supplier<ConditionEvaluationReport> reportSupplier;
			if (context instanceof GenericApplicationContext) {
				// Get the report early when the context allows early access to the bean
				// factory in case the context subsequently fails to load
				ConditionEvaluationReport report = getReport();
				reportSupplier = () -> report;
			}
			else {
				reportSupplier = this::getReport;
			}
			this.logger = new ConditionEvaluationReportLogger(
					ConditionEvaluationReportLoggingListener.this.logLevelForReport, reportSupplier);
		}

		private ConditionEvaluationReport getReport() {
			return ConditionEvaluationReport.get(this.context.getBeanFactory());
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		@Override
		public boolean supportsEventType(ResolvableType resolvableType) {
			Class<?> type = resolvableType.getRawClass();
			if (type == null) {
				return false;
			}
			return ContextRefreshedEvent.class.isAssignableFrom(type)
					|| ApplicationFailedEvent.class.isAssignableFrom(type);
		}

		@Override
		public boolean supportsSourceType(Class<?> sourceType) {
			return true;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
				if (contextRefreshedEvent.getApplicationContext() == this.context) {
					this.logger.logReport(false);
				}
			}
			else if (event instanceof ApplicationFailedEvent applicationFailedEvent
					&& applicationFailedEvent.getApplicationContext() == this.context) {
				this.logger.logReport(true);
			}
		}

	}

}
