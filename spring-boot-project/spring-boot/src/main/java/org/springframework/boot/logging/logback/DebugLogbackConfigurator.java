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

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;

/**
 * Custom {@link LogbackConfigurator} used to add {@link Status Statuses} when Logback
 * debugging is enabled.
 *
 * @author Andy Wilkinson
 */
class DebugLogbackConfigurator extends LogbackConfigurator {

	/**
	 * Constructs a new DebugLogbackConfigurator with the specified LoggerContext.
	 * @param context the LoggerContext to be used by the DebugLogbackConfigurator
	 */
	DebugLogbackConfigurator(LoggerContext context) {
		super(context);
	}

	/**
	 * Adds a conversion rule for a specific word using the provided converter class.
	 * @param conversionWord the word to be converted
	 * @param converterClass the class implementing the Converter interface for the
	 * conversion
	 * @throws IllegalArgumentException if the conversionWord or converterClass is null
	 *
	 * @since 1.0
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
		info("Adding conversion rule of type '" + converterClass.getName() + "' for word '" + conversionWord + "'");
		super.conversionRule(conversionWord, converterClass);
	}

	/**
	 * Adds an appender to the Logback configuration.
	 * @param name the name of the appender
	 * @param appender the appender to be added
	 */
	@Override
	public void appender(String name, Appender<?> appender) {
		info("Adding appender '" + appender + "' named '" + name + "'");
		super.appender(name, appender);
	}

	/**
	 * Configures a logger with the specified name, level, additive flag, and appender.
	 * @param name the name of the logger
	 * @param level the level of the logger
	 * @param additive the additive flag indicating whether the logger should inherit
	 * appenders from its ancestors
	 * @param appender the appender to be added to the logger
	 */
	@Override
	public void logger(String name, Level level, boolean additive, Appender<ILoggingEvent> appender) {
		info("Configuring logger '" + name + "' with level '" + level + "'. Additive: " + additive);
		if (appender != null) {
			info("Adding appender '" + appender + "' to logger '" + name + "'");
		}
		super.logger(name, level, additive, appender);
	}

	/**
	 * Starts the specified LifeCycle.
	 * @param lifeCycle the LifeCycle to start
	 */
	@Override
	public void start(LifeCycle lifeCycle) {
		info("Starting '" + lifeCycle + "'");
		super.start(lifeCycle);
	}

	/**
	 * Logs an informational message.
	 * @param message the message to be logged
	 */
	private void info(String message) {
		getContext().getStatusManager().add(new InfoStatus(message, this));
	}

}
