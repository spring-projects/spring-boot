/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcomes;
import org.springframework.boot.event.ApplicationFailedEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} and {@link SmartApplicationListener} that writes
 * the {@link AutoConfigurationReport} to the log. Reports are logged at the
 * {@link LogLevel#DEBUG DEBUG} level unless there was a problem, in which case they are
 * the {@link LogLevel#INFO INFO} level is used.
 * <p>
 * This initializer is not intended to be shared across multiple application context
 * instances.
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 */
public class AutoConfigurationReportLoggingInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>,
		SmartApplicationListener {

	private final Log logger = LogFactory.getLog(getClass());

	private ConfigurableApplicationContext applicationContext;

	private AutoConfigurationReport report;

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext instanceof GenericApplicationContext) {
			// Get the report early in case the context fails to load
			this.report = AutoConfigurationReport.get(this.applicationContext
					.getBeanFactory());
		}
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> type) {
		return ContextRefreshedEvent.class.isAssignableFrom(type)
				|| ApplicationFailedEvent.class.isAssignableFrom(type);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			if (((ApplicationContextEvent) event).getApplicationContext() == this.applicationContext) {
				logAutoConfigurationReport();
			}
		}
		else if (event instanceof ApplicationFailedEvent) {
			if (((ApplicationFailedEvent) event).getApplicationContext() == this.applicationContext) {
				logAutoConfigurationReport(true);
			}
		}
	}

	private void logAutoConfigurationReport() {
		logAutoConfigurationReport(!this.applicationContext.isActive());
	}

	public void logAutoConfigurationReport(boolean isCrashReport) {
		if (this.report == null) {
			if (this.applicationContext == null) {
				this.logger.info("Unable to provide auto-configuration report "
						+ "due to missing ApplicationContext");
				return;
			}
			this.report = AutoConfigurationReport.get(this.applicationContext
					.getBeanFactory());
		}
		if (this.report.getConditionAndOutcomesBySource().size() > 0) {
			if (isCrashReport && this.logger.isInfoEnabled()
					&& !this.logger.isDebugEnabled()) {
				this.logger.info("\n\nError starting ApplicationContext. "
						+ "To display the auto-configuration report enabled "
						+ "debug logging (start with --debug)\n\n");
			}
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(getLogMessage(this.report
						.getConditionAndOutcomesBySource()));
			}
		}
	}

	private StringBuilder getLogMessage(Map<String, ConditionAndOutcomes> outcomes) {
		StringBuilder message = new StringBuilder();
		message.append("\n\n\n");
		message.append("=========================\n");
		message.append("AUTO-CONFIGURATION REPORT\n");
		message.append("=========================\n\n\n");
		message.append("Positive matches:\n");
		message.append("-----------------\n");
		for (Map.Entry<String, ConditionAndOutcomes> entry : outcomes.entrySet()) {
			if (entry.getValue().isFullMatch()) {
				addLogMessage(message, entry.getKey(), entry.getValue());
			}
		}
		message.append("\n\n");
		message.append("Negative matches:\n");
		message.append("-----------------\n");
		for (Map.Entry<String, ConditionAndOutcomes> entry : outcomes.entrySet()) {
			if (!entry.getValue().isFullMatch()) {
				addLogMessage(message, entry.getKey(), entry.getValue());
			}
		}
		message.append("\n\n");
		return message;
	}

	private void addLogMessage(StringBuilder message, String source,
			ConditionAndOutcomes conditionAndOutcomes) {
		message.append("\n   " + ClassUtils.getShortName(source) + "\n");
		for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
			message.append("      - ");
			if (StringUtils.hasLength(conditionAndOutcome.getOutcome().getMessage())) {
				message.append(conditionAndOutcome.getOutcome().getMessage());
			}
			else {
				message.append(conditionAndOutcome.getOutcome().isMatch() ? "matched"
						: "did not match");
			}
			message.append(" (");
			message.append(ClassUtils.getShortName(conditionAndOutcome.getCondition()
					.getClass()));
			message.append(")\n");
		}

	}

}
