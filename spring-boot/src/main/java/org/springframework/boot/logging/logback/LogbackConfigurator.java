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
import ch.qos.logback.core.spi.PropertyContainer;

import org.springframework.util.Assert;

/**
 * Allows programmatic configuration of logback which is usually faster than parsing XML.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class LogbackConfigurator {

	private LoggerContext context;

	LogbackConfigurator(LoggerContext context) {
		Assert.notNull(context, "Context must not be null");
		this.context = context;
	}

	public PropertyContainer getContext() {
		return this.context;
	}

	public Object getConfigurationLock() {
		return this.context.getConfigurationLock();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void conversionRule(String conversionWord,
			Class<? extends Converter> converterClass) {
		Assert.hasLength(conversionWord, "Conversion word must not be empty");
		Assert.notNull(converterClass, "Converter class must not be null");
		Map<String, String> registry = (Map<String, String>) this.context
				.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
		if (registry == null) {
			registry = new HashMap<String, String>();
			this.context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
		}
		registry.put(conversionWord, converterClass.getName());
	}

	public void appender(String name, Appender<?> appender) {
		appender.setName(name);
		start(appender);
	}

	public void logger(String name, Level level) {
		logger(name, level, true);
	}

	public void logger(String name, Level level, boolean additive) {
		logger(name, level, additive, null);
	}

	public void logger(String name, Level level, boolean additive,
			Appender<ILoggingEvent> appender) {
		Logger logger = this.context.getLogger(name);
		if (level != null) {
			logger.setLevel(level);
		}
		logger.setAdditive(additive);
		if (appender != null) {
			logger.addAppender(appender);
		}
	}

	@SafeVarargs
	public final void root(Level level, Appender<ILoggingEvent>... appenders) {
		Logger logger = this.context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (level != null) {
			logger.setLevel(level);
		}
		for (Appender<ILoggingEvent> appender : appenders) {
			logger.addAppender(appender);
		}
	}

	public void start(LifeCycle lifeCycle) {
		if (lifeCycle instanceof ContextAware) {
			((ContextAware) lifeCycle).setContext(this.context);
		}
		lifeCycle.start();
	}

}
