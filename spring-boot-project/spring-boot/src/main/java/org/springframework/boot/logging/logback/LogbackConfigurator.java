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

package org.springframework.boot.logging.logback;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

import org.springframework.util.Assert;

/**
 * Allows programmatic configuration of logback which is usually faster than parsing XML.
 *
 * @author Phillip Webb
 */
class LogbackConfigurator {

	private final LoggerContext context;

	/**
	 * Constructs a new LogbackConfigurator with the specified LoggerContext.
	 * @param context the LoggerContext to be used by the configurator (must not be null)
	 * @throws IllegalArgumentException if the context is null
	 */
	LogbackConfigurator(LoggerContext context) {
		Assert.notNull(context, "Context must not be null");
		this.context = context;
	}

	/**
	 * Returns the LoggerContext associated with this LogbackConfigurator.
	 * @return the LoggerContext associated with this LogbackConfigurator
	 */
	LoggerContext getContext() {
		return this.context;
	}

	/**
	 * Retrieves the configuration lock object.
	 * @return the configuration lock object
	 */
	Object getConfigurationLock() {
		return this.context.getConfigurationLock();
	}

	/**
	 * Registers a conversion rule for a specific conversion word and converter class.
	 * @param conversionWord the conversion word to be registered
	 * @param converterClass the converter class to be registered
	 * @throws IllegalArgumentException if the conversion word is empty or the converter
	 * class is null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
		Assert.hasLength(conversionWord, "Conversion word must not be empty");
		Assert.notNull(converterClass, "Converter class must not be null");
		Map<String, String> registry = (Map<String, String>) this.context
			.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
		if (registry == null) {
			registry = new HashMap<>();
			this.context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
		}
		registry.put(conversionWord, converterClass.getName());
	}

	/**
	 * Appends the specified appender to the LogbackConfigurator with the given name.
	 * @param name the name of the appender
	 * @param appender the appender to be appended
	 */
	void appender(String name, Appender<?> appender) {
		appender.setName(name);
		start(appender);
	}

	/**
	 * Logs a message with the specified name and level.
	 * @param name the name of the logger
	 * @param level the level of the log message
	 */
	void logger(String name, Level level) {
		logger(name, level, true);
	}

	/**
	 * Sets up a logger with the specified name, level, and additive flag.
	 * @param name the name of the logger
	 * @param level the logging level for the logger
	 * @param additive the additive flag for the logger
	 */
	void logger(String name, Level level, boolean additive) {
		logger(name, level, additive, null);
	}

	/**
	 * Configures a logger with the specified name, level, additive flag, and appender.
	 * @param name the name of the logger
	 * @param level the level of the logger (can be null)
	 * @param additive the additive flag indicating whether the logger should inherit
	 * appenders from its ancestors
	 * @param appender the appender to be added to the logger (can be null)
	 */
	void logger(String name, Level level, boolean additive, Appender<ILoggingEvent> appender) {
		Logger logger = this.context.getLogger(name);
		if (level != null) {
			logger.setLevel(level);
		}
		logger.setAdditive(additive);
		if (appender != null) {
			logger.addAppender(appender);
		}
	}

	/**
	 * Sets the root logger level and adds appenders to the root logger.
	 * @param level the level to set for the root logger
	 * @param appenders the appenders to add to the root logger
	 */
	@SafeVarargs
	final void root(Level level, Appender<ILoggingEvent>... appenders) {
		Logger logger = this.context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (level != null) {
			logger.setLevel(level);
		}
		for (Appender<ILoggingEvent> appender : appenders) {
			logger.addAppender(appender);
		}
	}

	/**
	 * Starts the given LifeCycle object.
	 * @param lifeCycle the LifeCycle object to start
	 */
	void start(LifeCycle lifeCycle) {
		if (lifeCycle instanceof ContextAware contextAware) {
			contextAware.setContext(this.context);
		}
		lifeCycle.start();
	}

}
