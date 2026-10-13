/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.config.Configuration;
import org.jspecify.annotations.Nullable;

class TestLog4J2LoggingSystem extends Log4J2LoggingSystem {

	private boolean disableSelfInitialization;

	TestLog4J2LoggingSystem(String contextName) {
		// Tests add resources to the thread context classloader
		super(Thread.currentThread().getContextClassLoader());
		getLoggerContext().setName(contextName);
		getLoggerContext().start();
	}

	Configuration getConfiguration() {
		return getLoggerContext().getConfiguration();
	}

	@Override
	protected @Nullable String getSelfInitializationConfig() {
		return this.disableSelfInitialization ? null : super.getSelfInitializationConfig();
	}

	void disableSelfInitialization() {
		this.disableSelfInitialization = true;
	}

}
