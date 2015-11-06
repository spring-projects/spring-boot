/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that writes the {@link ConditionEvaluationReport}
 * to the log. Reports are logged at the {@link LogLevel#DEBUG DEBUG} level unless there
 * was a problem, in which case they are the {@link LogLevel#INFO INFO} level is used.
 * <p>
 * This initializer is not intended to be shared across multiple application context
 * instances.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class AutoConfigurationReportLoggingInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private final Log logger = LogFactory.getLog(getClass());

	private ConfigurableApplicationContext applicationContext;

	private ConditionEvaluationReport report;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		applicationContext.addApplicationListener(new AutoConfigurationReportListener());
		if (applicationContext instanceof GenericApplicationContext) {
			// Get the report early in case the context fails to load
			this.report = ConditionEvaluationReport
					.get(this.applicationContext.getBeanFactory());
		}
	}

	protected void onApplicationEvent(ApplicationEvent event) {
		ConfigurableApplicationContext initializerApplicationContext = AutoConfigurationReportLoggingInitializer.this.applicationContext;
		if (event instanceof ContextRefreshedEvent) {
			if (((ApplicationContextEvent) event)
					.getApplicationContext() == initializerApplicationContext) {
				logAutoConfigurationReport();
			}
		}
		else if (event instanceof ApplicationFailedEvent) {
			if (((ApplicationFailedEvent) event)
					.getApplicationContext() == initializerApplicationContext) {
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
			this.report = ConditionEvaluationReport
					.get(this.applicationContext.getBeanFactory());
		}
		if (this.report.getConditionAndOutcomesBySource().size() > 0) {
			if (isCrashReport && this.logger.isInfoEnabled()
					&& !this.logger.isDebugEnabled()) {
				this.logger.info("\n\nError starting ApplicationContext. "
						+ "To display the auto-configuration report enable "
						+ "debug logging (start with --debug)\n\n");
			}
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(getLogMessage(this.report));
			}
		}
	}

	private StringBuilder getLogMessage(ConditionEvaluationReport report) {
		StringBuilder message = new StringBuilder();
		message.append("\n\n\n");
		message.append("=========================\n");
		message.append("AUTO-CONFIGURATION REPORT\n");
		message.append("=========================\n\n\n");
		message.append("Positive matches:\n");
		message.append("-----------------\n");
		Map<String, ConditionAndOutcomes> shortOutcomes = orderByName(
				report.getConditionAndOutcomesBySource());
		for (Map.Entry<String, ConditionAndOutcomes> entry : shortOutcomes.entrySet()) {
			if (entry.getValue().isFullMatch()) {
				addLogMessage(message, entry.getKey(), entry.getValue());
			}
		}
		message.append("\n\n");
		message.append("Negative matches:\n");
		message.append("-----------------\n");
		for (Map.Entry<String, ConditionAndOutcomes> entry : shortOutcomes.entrySet()) {
			if (!entry.getValue().isFullMatch()) {
				addLogMessage(message, entry.getKey(), entry.getValue());
			}
		}
		message.append("\n\n");
		message.append("Exclusions:\n");
		message.append("-----------\n");
		if (report.getExclusions().isEmpty()) {
			message.append("\n    None\n");
		}
		else {
			for (String exclusion : report.getExclusions()) {
				message.append("\n   " + exclusion + "\n");
			}
		}
		message.append("\n\n");
		message.append("Unconditional classes:\n");
		message.append("----------------------\n");
		if (report.getUnconditionalClasses().isEmpty()) {
			message.append("\n    None\n");
		}
		else {
			for (String unconditionalClass : report.getUnconditionalClasses()) {
				message.append("\n   " + unconditionalClass + "\n");
			}
		}
		message.append("\n\n");
		return message;
	}

	private Map<String, ConditionAndOutcomes> orderByName(
			Map<String, ConditionAndOutcomes> outcomes) {
		Map<String, ConditionAndOutcomes> result = new LinkedHashMap<String, ConditionAndOutcomes>();
		List<String> names = new ArrayList<String>();
		Map<String, String> classNames = new HashMap<String, String>();
		for (String name : outcomes.keySet()) {
			String shortName = ClassUtils.getShortName(name);
			names.add(shortName);
			classNames.put(shortName, name);
		}
		Collections.sort(names);
		for (String shortName : names) {
			result.put(shortName, outcomes.get(classNames.get(shortName)));
		}
		return result;
	}

	private void addLogMessage(StringBuilder message, String source,
			ConditionAndOutcomes conditionAndOutcomes) {
		message.append("\n   " + source);
		message.append(
				conditionAndOutcomes.isFullMatch() ? " matched\n" : " did not match\n");
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
			message.append(ClassUtils
					.getShortName(conditionAndOutcome.getCondition().getClass()));
			message.append(")\n");
		}

	}

	private class AutoConfigurationReportListener implements GenericApplicationListener {

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
			AutoConfigurationReportLoggingInitializer.this.onApplicationEvent(event);
		}

	}

}
