/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

import ch.qos.logback.classic.Level;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.ClassUtils;

/**
 * Utility to initialize logback (when present) to use INFO level logging.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public abstract class LogbackInitializer {

	public static void initialize() {
		if (ClassUtils.isPresent("org.slf4j.LoggerFactory", null)
				&& ClassUtils.isPresent("ch.qos.logback.classic.Logger", null)) {
			new Initializer().setRootLogLevel();
		}
	}

	private static final class Initializer {

		void setRootLogLevel() {
			ILoggerFactory factory = LoggerFactory.getILoggerFactory();
			Logger logger = factory.getLogger(Logger.ROOT_LOGGER_NAME);
			((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
		}

	}

}
