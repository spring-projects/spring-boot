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

package org.springframework.boot.logging;

import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations that utilize SLF4J.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public abstract class Slf4JLoggingSystem extends AbstractLoggingSystem {

	private static final String SLF4J_BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

	private static final String LOG4J_BRIDGE_HANDLER = "org.apache.logging.log4j.jul.Log4jBridgeHandler";

	private static final String LOG4J_LOG_MANAGER = "org.apache.logging.log4j.jul.LogManager";

	private final List<BridgeHandler> handlers;

	public Slf4JLoggingSystem(ClassLoader classLoader) {
		this(classLoader, Collections.singletonList(new Slf4jHandler(classLoader)));
	}

	protected Slf4JLoggingSystem(ClassLoader classLoader, List<BridgeHandler> handlers) {
		super(classLoader);
		this.handlers = handlers;
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();
		configureJdkLoggingBridgeHandler();
	}

	@Override
	public void cleanUp() {
		this.handlers.stream().filter(BridgeHandler::isAvailable).forEach(BridgeHandler::remove);
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		if (initializationContext != null) {
			applySystemProperties(initializationContext.getEnvironment(), logFile);
		}
	}

	private void configureJdkLoggingBridgeHandler() {
		try {
			if (isJulUsingASingleConsoleHandlerAtMost() && !isLog4jLogManagerInstalled()) {
				this.handlers.stream().filter(BridgeHandler::isAvailable).findFirst().ifPresent(BridgeHandler::install);
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	/**
	 * Return whether bridging JUL into SLF4J or not.
	 * @return whether bridging JUL into SLF4J or not
	 * @since 2.0.4
	 */
	protected final boolean isBridgeJulIntoSlf4j() {
		return this.handlers.stream().filter(Slf4jHandler.class::isInstance).anyMatch(BridgeHandler::isAvailable)
				&& isJulUsingASingleConsoleHandlerAtMost();
	}

	protected final boolean isBridgeHandlerAvailable() {
		return this.handlers.stream().anyMatch(BridgeHandler::isAvailable);
	}

	private boolean isJulUsingASingleConsoleHandlerAtMost() {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
	}

	private boolean isLog4jLogManagerInstalled() {
		final String logManagerClassName = LogManager.getLogManager().getClass().getName();
		return LOG4J_LOG_MANAGER.equals(logManagerClassName);
	}

	static void removeDefaultRootHandler() {
		try {
			Logger rootLogger = LogManager.getLogManager().getLogger("");
			Handler[] handlers = rootLogger.getHandlers();
			if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
				rootLogger.removeHandler(handlers[0]);
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	public interface BridgeHandler {

		boolean isAvailable();

		void install();

		void remove();

	}

	protected static class Slf4jHandler implements BridgeHandler {

		private final ClassLoader classLoader;

		public Slf4jHandler(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public boolean isAvailable() {
			return ClassUtils.isPresent(SLF4J_BRIDGE_HANDLER, this.classLoader);
		}

		@Override
		public void install() {
			removeDefaultRootHandler();
			SLF4JBridgeHandler.install();
		}

		@Override
		public void remove() {
			SLF4JBridgeHandler.uninstall();
		}

	}

	protected static class Log4jHandler implements BridgeHandler {

		private final ClassLoader classLoader;

		public Log4jHandler(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public boolean isAvailable() {
			return ClassUtils.isPresent(LOG4J_BRIDGE_HANDLER, this.classLoader);
		}

		@Override
		public void install() {
			Log4jBridgeHandler.install(true, null, true);
		}

		@Override
		public void remove() {
			Logger rootLogger = LogManager.getLogManager().getLogger("");
			for (final Handler handler : rootLogger.getHandlers()) {
				if (handler instanceof Log4jBridgeHandler) {
					handler.close();
					rootLogger.removeHandler(handler);
				}
			}
		}

	}

}
