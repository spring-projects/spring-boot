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

	/**
	 * Constructs a new ConditionEvaluationReportLoggingListener with the default log
	 * level of DEBUG.
	 */
	public ConditionEvaluationReportLoggingListener() {
		this(LogLevel.DEBUG);
	}

	/**
	 * Constructs a new ConditionEvaluationReportLoggingListener with the specified log
	 * level for report.
	 * @param logLevelForReport the log level for the report, must be INFO or DEBUG
	 * @throws IllegalArgumentException if the log level is not INFO or DEBUG
	 */
	private ConditionEvaluationReportLoggingListener(LogLevel logLevelForReport) {
		Assert.isTrue(isInfoOrDebug(logLevelForReport), "LogLevel must be INFO or DEBUG");
		this.logLevelForReport = logLevelForReport;
	}

	/**
	 * Checks if the given log level is either INFO or DEBUG.
	 * @param logLevelForReport the log level to be checked
	 * @return true if the log level is INFO or DEBUG, false otherwise
	 */
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

	/**
	 * Initializes the ConditionEvaluationReportLoggingListener by adding an
	 * ApplicationListener to the given ApplicationContext. This ApplicationListener is
	 * responsible for logging the condition evaluation report.
	 * @param applicationContext The ConfigurableApplicationContext to which the
	 * ApplicationListener will be added.
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(new ConditionEvaluationReportListener(applicationContext));
	}

	/**
	 * ConditionEvaluationReportListener class.
	 */
	private final class ConditionEvaluationReportListener implements GenericApplicationListener {

		private final ConfigurableApplicationContext context;

		private final ConditionEvaluationReportLogger logger;

		/**
		 * Constructs a new ConditionEvaluationReportListener with the given
		 * ConfigurableApplicationContext.
		 * @param context the ConfigurableApplicationContext to be used by the listener
		 */
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

		/**
		 * Returns the ConditionEvaluationReport generated by the
		 * ConditionEvaluationReportListener.
		 * @return the ConditionEvaluationReport generated by the
		 * ConditionEvaluationReportListener
		 */
		private ConditionEvaluationReport getReport() {
			return ConditionEvaluationReport.get(this.context.getBeanFactory());
		}

		/**
		 * Returns the order in which this listener should be invoked during the condition
		 * evaluation process. The listener with the lowest precedence will be invoked
		 * first.
		 * @return the order of this listener
		 */
		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		/**
		 * Determines whether the specified event type is supported by this listener.
		 * @param resolvableType the ResolvableType representing the event type
		 * @return true if the event type is supported, false otherwise
		 */
		@Override
		public boolean supportsEventType(ResolvableType resolvableType) {
			Class<?> type = resolvableType.getRawClass();
			if (type == null) {
				return false;
			}
			return ContextRefreshedEvent.class.isAssignableFrom(type)
					|| ApplicationFailedEvent.class.isAssignableFrom(type);
		}

		/**
		 * Returns true if the specified source type is supported.
		 * @param sourceType the source type to check
		 * @return true if the source type is supported, false otherwise
		 */
		@Override
		public boolean supportsSourceType(Class<?> sourceType) {
			return true;
		}

		/**
		 * This method is an implementation of the onApplicationEvent method declared in
		 * the ConditionEvaluationReportListener class. It is called when an application
		 * event is triggered.
		 * @param event The application event that is triggered.
		 */
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
