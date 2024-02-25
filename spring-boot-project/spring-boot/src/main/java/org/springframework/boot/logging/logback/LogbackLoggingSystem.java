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

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.TurboFilterList;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.helpers.SubstituteLoggerFactory;

import org.springframework.aot.AotDetector;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for <a href="https://logback.qos.ch">logback</a>.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Ben Hale
 * @since 1.0.0
 */
public class LogbackLoggingSystem extends AbstractLoggingSystem implements BeanFactoryInitializationAotProcessor {

	private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

	private static final String CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";

	private static final LogLevels<Level> LEVELS = new LogLevels<>();

	static {
		LEVELS.map(LogLevel.TRACE, Level.TRACE);
		LEVELS.map(LogLevel.TRACE, Level.ALL);
		LEVELS.map(LogLevel.DEBUG, Level.DEBUG);
		LEVELS.map(LogLevel.INFO, Level.INFO);
		LEVELS.map(LogLevel.WARN, Level.WARN);
		LEVELS.map(LogLevel.ERROR, Level.ERROR);
		LEVELS.map(LogLevel.FATAL, Level.ERROR);
		LEVELS.map(LogLevel.OFF, Level.OFF);
	}

	private static final TurboFilter FILTER = new TurboFilter() {

		/**
		 * This method is used to decide whether to log a specific log event or not.
		 * @param marker The marker associated with the log event.
		 * @param logger The logger instance associated with the log event.
		 * @param level The log level of the log event.
		 * @param format The log message format.
		 * @param params The parameters to be used in the log message format.
		 * @param t The throwable associated with the log event.
		 * @return The decision on whether to log the event or not.
		 */
		@Override
		public FilterReply decide(Marker marker, ch.qos.logback.classic.Logger logger, Level level, String format,
				Object[] params, Throwable t) {
			return FilterReply.DENY;
		}

	};

	/**
	 * Constructs a new LogbackLoggingSystem with the specified class loader.
	 * @param classLoader the class loader to be used by the logging system
	 */
	public LogbackLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	/**
	 * Returns the system properties for the logging system.
	 * @param environment the configurable environment
	 * @return the logging system properties
	 */
	@Override
	public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
		return new LogbackLoggingSystemProperties(environment, getDefaultValueResolver(environment), null);
	}

	/**
	 * Returns an array of standard configuration locations for LogbackLoggingSystem. The
	 * standard configuration locations are "logback-test.groovy", "logback-test.xml",
	 * "logback.groovy", and "logback.xml".
	 * @return an array of standard configuration locations
	 */
	@Override
	protected String[] getStandardConfigLocations() {
		return new String[] { "logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml" };
	}

	/**
	 * This method is called before the initialization of the LogbackLoggingSystem. It
	 * checks if the logger context is already initialized and if not, it performs the
	 * necessary configurations. It adds a JDK logging bridge handler and a turbo filter
	 * to the logger context.
	 */
	@Override
	public void beforeInitialize() {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		super.beforeInitialize();
		configureJdkLoggingBridgeHandler();
		loggerContext.getTurboFilterList().add(FILTER);
	}

	/**
	 * Configures the JDK logging bridge handler. This method checks if the bridge between
	 * JUL (Java Util Logging) and SLF4J (Simple Logging Facade for Java) is enabled. If
	 * enabled, it removes any existing JDK logging bridge handler and installs the SLF4J
	 * bridge handler.
	 * @throws Throwable if an error occurs while configuring the JDK logging bridge
	 * handler.
	 * @see SLF4JBridgeHandler
	 * @see #isBridgeJulIntoSlf4j()
	 * @see #removeJdkLoggingBridgeHandler()
	 */
	private void configureJdkLoggingBridgeHandler() {
		try {
			if (isBridgeJulIntoSlf4j()) {
				removeJdkLoggingBridgeHandler();
				SLF4JBridgeHandler.install();
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	/**
	 * Checks if the bridge between JUL (Java Util Logging) and SLF4J (Simple Logging
	 * Facade for Java) is enabled.
	 * @return {@code true} if the bridge is enabled and JUL is using a single console
	 * handler at most, {@code false} otherwise.
	 */
	private boolean isBridgeJulIntoSlf4j() {
		return isBridgeHandlerAvailable() && isJulUsingASingleConsoleHandlerAtMost();
	}

	/**
	 * Checks if the bridge handler is available.
	 * @return {@code true} if the bridge handler is available, {@code false} otherwise.
	 */
	private boolean isBridgeHandlerAvailable() {
		return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
	}

	/**
	 * Checks if the JUL (Java Util Logging) is using a single ConsoleHandler at most.
	 * @return true if JUL is using a single ConsoleHandler at most, false otherwise
	 */
	private boolean isJulUsingASingleConsoleHandlerAtMost() {
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
	}

	/**
	 * Removes the JDK logging bridge handler and uninstalls the SLF4J bridge handler.
	 * This method attempts to remove the default root handler and then uninstalls the
	 * SLF4J bridge handler. If any exception occurs during the process, it is ignored and
	 * the execution continues.
	 */
	private void removeJdkLoggingBridgeHandler() {
		try {
			removeDefaultRootHandler();
			SLF4JBridgeHandler.uninstall();
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	/**
	 * Removes the default root handler from the LogManager. This method checks if the
	 * root logger has only one handler, which is an instance of ConsoleHandler, and
	 * removes it if true.
	 * @throws Throwable if an error occurs while removing the default root handler
	 *
	 * @since 1.0.0
	 */
	private void removeDefaultRootHandler() {
		try {
			java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
			Handler[] handlers = rootLogger.getHandlers();
			if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
				rootLogger.removeHandler(handlers[0]);
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	/**
	 * Initializes the logging system.
	 * @param initializationContext The logging initialization context.
	 * @param configLocation The location of the logging configuration file.
	 * @param logFile The log file to be used.
	 */
	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		if (!initializeFromAotGeneratedArtifactsIfPossible(initializationContext, logFile)) {
			super.initialize(initializationContext, configLocation, logFile);
		}
		loggerContext.putObject(Environment.class.getName(), initializationContext.getEnvironment());
		loggerContext.getTurboFilterList().remove(FILTER);
		markAsInitialized(loggerContext);
		if (StringUtils.hasText(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
			getLogger(LogbackLoggingSystem.class.getName()).warn("Ignoring '" + CONFIGURATION_FILE_PROPERTY
					+ "' system property. Please use 'logging.config' instead.");
		}
	}

	/**
	 * Initializes the logging system using the AOT generated artifacts if possible.
	 * @param initializationContext the logging initialization context
	 * @param logFile the log file
	 * @return {@code true} if the logging system is successfully initialized using the
	 * AOT generated artifacts, {@code false} otherwise
	 */
	private boolean initializeFromAotGeneratedArtifactsIfPossible(LoggingInitializationContext initializationContext,
			LogFile logFile) {
		if (!AotDetector.useGeneratedArtifacts()) {
			return false;
		}
		if (initializationContext != null) {
			applySystemProperties(initializationContext.getEnvironment(), logFile);
		}
		LoggerContext loggerContext = getLoggerContext();
		stopAndReset(loggerContext);
		SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
		configurator.setContext(loggerContext);
		boolean configuredUsingAotGeneratedArtifacts = configurator.configureUsingAotGeneratedArtifacts();
		if (configuredUsingAotGeneratedArtifacts) {
			reportConfigurationErrorsIfNecessary(loggerContext);
		}
		return configuredUsingAotGeneratedArtifacts;
	}

	/**
	 * Loads the default logging configuration for Logback.
	 * @param initializationContext the logging initialization context
	 * @param logFile the log file to be used
	 */
	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		LoggerContext context = getLoggerContext();
		stopAndReset(context);
		withLoggingSuppressed(() -> {
			boolean debug = Boolean.getBoolean("logback.debug");
			if (debug) {
				StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
			}
			Environment environment = initializationContext.getEnvironment();
			// Apply system properties directly in case the same JVM runs multiple apps
			new LogbackLoggingSystemProperties(environment, getDefaultValueResolver(environment), context::putProperty)
				.apply(logFile);
			LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context)
					: new LogbackConfigurator(context);
			new DefaultLogbackConfiguration(logFile).apply(configurator);
			context.setPackagingDataEnabled(true);
		});
	}

	/**
	 * Loads the configuration for Logback logging.
	 * @param initializationContext The logging initialization context.
	 * @param location The location of the configuration file.
	 * @param logFile The log file to be used.
	 */
	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		stopAndReset(loggerContext);
		withLoggingSuppressed(() -> {
			if (initializationContext != null) {
				applySystemProperties(initializationContext.getEnvironment(), logFile);
			}
			try {
				configureByResourceUrl(initializationContext, loggerContext, ResourceUtils.getURL(location));
			}
			catch (Exception ex) {
				throw new IllegalStateException("Could not initialize Logback logging from " + location, ex);
			}
		});
		reportConfigurationErrorsIfNecessary(loggerContext);
	}

	/**
	 * Reports any configuration errors in the logger context, if necessary.
	 * @param loggerContext the logger context to check for configuration errors
	 * @throws IllegalStateException if any configuration errors are detected
	 */
	private void reportConfigurationErrorsIfNecessary(LoggerContext loggerContext) {
		StringBuilder errors = new StringBuilder();
		List<Throwable> suppressedExceptions = new ArrayList<>();
		for (Status status : loggerContext.getStatusManager().getCopyOfStatusList()) {
			if (status.getLevel() == Status.ERROR) {
				errors.append((!errors.isEmpty()) ? String.format("%n") : "");
				errors.append(status);
				if (status.getThrowable() != null) {
					suppressedExceptions.add(status.getThrowable());
				}
			}
		}
		if (errors.isEmpty()) {
			if (!StatusUtil.contextHasStatusListener(loggerContext)) {
				StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
			}
			return;
		}
		IllegalStateException ex = new IllegalStateException(
				String.format("Logback configuration error detected: %n%s", errors));
		suppressedExceptions.forEach(ex::addSuppressed);
		throw ex;
	}

	/**
	 * Configures the logging system using a resource URL.
	 * @param initializationContext the logging initialization context
	 * @param loggerContext the logger context
	 * @param url the resource URL to configure the logging system
	 * @throws JoranException if an error occurs during configuration
	 * @throws IllegalArgumentException if the file extension of the URL is not supported
	 */
	private void configureByResourceUrl(LoggingInitializationContext initializationContext, LoggerContext loggerContext,
			URL url) throws JoranException {
		if (url.getPath().endsWith(".xml")) {
			JoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
			configurator.setContext(loggerContext);
			configurator.doConfigure(url);
		}
		else {
			throw new IllegalArgumentException("Unsupported file extension in '" + url + "'. Only .xml is supported");
		}
	}

	/**
	 * Stops and resets the given logger context.
	 * @param loggerContext the logger context to stop and reset
	 */
	private void stopAndReset(LoggerContext loggerContext) {
		loggerContext.stop();
		loggerContext.reset();
		if (isBridgeHandlerInstalled()) {
			addLevelChangePropagator(loggerContext);
		}
	}

	/**
	 * Checks if the SLF4JBridgeHandler is installed as the only handler in the root
	 * logger.
	 * @return true if the SLF4JBridgeHandler is installed as the only handler in the root
	 * logger, false otherwise
	 */
	private boolean isBridgeHandlerInstalled() {
		if (!isBridgeHandlerAvailable()) {
			return false;
		}
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 1 && handlers[0] instanceof SLF4JBridgeHandler;
	}

	/**
	 * Adds a LevelChangePropagator to the specified LoggerContext. This propagator is
	 * responsible for propagating log level changes from Logback to JUL (Java Util
	 * Logging).
	 * @param loggerContext the LoggerContext to which the LevelChangePropagator will be
	 * added
	 */
	private void addLevelChangePropagator(LoggerContext loggerContext) {
		LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
		levelChangePropagator.setResetJUL(true);
		levelChangePropagator.setContext(loggerContext);
		loggerContext.addListener(levelChangePropagator);
	}

	/**
	 * Cleans up the Logback logging system. This method is called to perform necessary
	 * clean-up tasks before shutting down the logging system. It clears the status
	 * manager and removes the turbo filter from the logger context. Additionally, if the
	 * bridge handler is available, it removes the JDK logging bridge handler.
	 *
	 * @see LoggerContext
	 * @see LoggerContext#markAsUninitialized(LoggerContext)
	 * @see LoggerContext#getStatusManager()
	 * @see LoggerContext#getTurboFilterList()
	 * @see LoggerContext#getTurboFilterList().remove(TurboFilter)
	 * @see LogbackLoggingSystem#isBridgeHandlerAvailable()
	 * @see LogbackLoggingSystem#removeJdkLoggingBridgeHandler()
	 * @see StatusManager#clear()
	 * @see TurboFilter
	 */
	@Override
	public void cleanUp() {
		LoggerContext context = getLoggerContext();
		markAsUninitialized(context);
		super.cleanUp();
		if (isBridgeHandlerAvailable()) {
			removeJdkLoggingBridgeHandler();
		}
		context.getStatusManager().clear();
		context.getTurboFilterList().remove(FILTER);
	}

	/**
	 * Reinitializes the logging system with the provided initialization context. This
	 * method resets the logger context, clears the status manager, and loads the
	 * configuration.
	 * @param initializationContext the logging initialization context
	 */
	@Override
	protected void reinitialize(LoggingInitializationContext initializationContext) {
		getLoggerContext().reset();
		getLoggerContext().getStatusManager().clear();
		loadConfiguration(initializationContext, getSelfInitializationConfig(), null);
	}

	/**
	 * Retrieves a list of logger configurations.
	 * @return A list of LoggerConfiguration objects representing the logger
	 * configurations.
	 */
	@Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		List<LoggerConfiguration> result = new ArrayList<>();
		for (ch.qos.logback.classic.Logger logger : getLoggerContext().getLoggerList()) {
			result.add(getLoggerConfiguration(logger));
		}
		result.sort(CONFIGURATION_COMPARATOR);
		return result;
	}

	/**
	 * Retrieves the configuration for a specific logger.
	 * @param loggerName the name of the logger
	 * @return the configuration for the specified logger
	 */
	@Override
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
		String name = getLoggerName(loggerName);
		LoggerContext loggerContext = getLoggerContext();
		return getLoggerConfiguration(loggerContext.exists(name));
	}

	/**
	 * Returns the logger name based on the provided name. If the provided name is null or
	 * empty, or if it is equal to the root logger name, the root logger name is returned.
	 * @param name the name of the logger
	 * @return the logger name
	 */
	private String getLoggerName(String name) {
		if (!StringUtils.hasLength(name) || Logger.ROOT_LOGGER_NAME.equals(name)) {
			return ROOT_LOGGER_NAME;
		}
		return name;
	}

	/**
	 * Retrieves the configuration of a logger.
	 * @param logger the logger to retrieve the configuration for
	 * @return the configuration of the logger, or null if the logger is null
	 */
	private LoggerConfiguration getLoggerConfiguration(ch.qos.logback.classic.Logger logger) {
		if (logger == null) {
			return null;
		}
		LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
		LogLevel effectiveLevel = LEVELS.convertNativeToSystem(logger.getEffectiveLevel());
		String name = getLoggerName(logger.getName());
		return new LoggerConfiguration(name, level, effectiveLevel);
	}

	/**
	 * Returns a set of supported log levels.
	 * @return a set of supported log levels
	 */
	@Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	/**
	 * Sets the log level for the specified logger.
	 * @param loggerName the name of the logger
	 * @param level the log level to be set
	 */
	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		ch.qos.logback.classic.Logger logger = getLogger(loggerName);
		if (logger != null) {
			logger.setLevel(LEVELS.convertSystemToNative(level));
		}
	}

	/**
	 * Returns a Runnable object that can be used as a shutdown handler. The shutdown
	 * handler stops the logger context.
	 * @return a Runnable object representing the shutdown handler
	 */
	@Override
	public Runnable getShutdownHandler() {
		return () -> getLoggerContext().stop();
	}

	/**
	 * Retrieves a logger with the specified name.
	 * @param name the name of the logger to retrieve
	 * @return the logger with the specified name
	 */
	private ch.qos.logback.classic.Logger getLogger(String name) {
		LoggerContext factory = getLoggerContext();
		return factory.getLogger(getLoggerName(name));
	}

	/**
	 * Retrieves the LoggerContext from the LoggerFactory.
	 * @return The LoggerContext instance.
	 * @throws IllegalStateException if the LoggerFactory is not an instance of
	 * LoggerContext.
	 * @throws IllegalStateException if Logback is on the classpath but there is a
	 * competing implementation.
	 * @throws IllegalStateException if using WebLogic and 'org.slf4j' is not added to
	 * prefer-application-packages in WEB-INF/weblogic.xml.
	 */
	private LoggerContext getLoggerContext() {
		ILoggerFactory factory = getLoggerFactory();
		Assert.isInstanceOf(LoggerContext.class, factory,
				() -> String.format(
						"LoggerFactory is not a Logback LoggerContext but Logback is on "
								+ "the classpath. Either remove Logback or the competing "
								+ "implementation (%s loaded from %s). If you are using "
								+ "WebLogic you will need to add 'org.slf4j' to "
								+ "prefer-application-packages in WEB-INF/weblogic.xml",
						factory.getClass(), getLocation(factory)));
		return (LoggerContext) factory;
	}

	/**
	 * Retrieves the logger factory used by the LogbackLoggingSystem.
	 * @return The logger factory used by the LogbackLoggingSystem.
	 * @throws IllegalStateException If interrupted while waiting for a non-substitute
	 * logger factory.
	 */
	private ILoggerFactory getLoggerFactory() {
		ILoggerFactory factory = LoggerFactory.getILoggerFactory();
		while (factory instanceof SubstituteLoggerFactory) {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for non-substitute logger factory", ex);
			}
			factory = LoggerFactory.getILoggerFactory();
		}
		return factory;
	}

	/**
	 * Retrieves the location of the given ILoggerFactory.
	 * @param factory the ILoggerFactory to retrieve the location for
	 * @return the location of the given ILoggerFactory, or "unknown location" if unable
	 * to determine
	 * @throws SecurityException if a security exception occurs while retrieving the
	 * location
	 */
	private Object getLocation(ILoggerFactory factory) {
		try {
			ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource != null) {
				return codeSource.getLocation();
			}
		}
		catch (SecurityException ex) {
			// Unable to determine location
		}
		return "unknown location";
	}

	/**
	 * Checks if the given LoggerContext has already been initialized.
	 * @param loggerContext the LoggerContext to check
	 * @return true if the LoggerContext has already been initialized, false otherwise
	 */
	private boolean isAlreadyInitialized(LoggerContext loggerContext) {
		return loggerContext.getObject(LoggingSystem.class.getName()) != null;
	}

	/**
	 * Marks the logger context as initialized.
	 * @param loggerContext the logger context to mark as initialized
	 */
	private void markAsInitialized(LoggerContext loggerContext) {
		loggerContext.putObject(LoggingSystem.class.getName(), new Object());
	}

	/**
	 * Marks the specified logger context as uninitialized by removing the LoggingSystem
	 * object from it.
	 * @param loggerContext the logger context to mark as uninitialized
	 */
	private void markAsUninitialized(LoggerContext loggerContext) {
		loggerContext.removeObject(LoggingSystem.class.getName());
	}

	/**
	 * Returns the default log correlation pattern.
	 * @return the default log correlation pattern ("%correlationId")
	 */
	@Override
	protected String getDefaultLogCorrelationPattern() {
		return "%correlationId";
	}

	/**
	 * This method processes the BeanFactoryInitializationAotContribution ahead of time.
	 * It retrieves the contribution object from the LoggerContext using the specified
	 * key. After retrieving the object, it removes it from the LoggerContext. Finally, it
	 * returns the retrieved contribution object.
	 * @param beanFactory The ConfigurableListableBeanFactory used for processing the
	 * contribution.
	 * @return The BeanFactoryInitializationAotContribution object retrieved from the
	 * LoggerContext.
	 */
	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		String key = BeanFactoryInitializationAotContribution.class.getName();
		LoggerContext context = getLoggerContext();
		BeanFactoryInitializationAotContribution contribution = (BeanFactoryInitializationAotContribution) context
			.getObject(key);
		context.removeObject(key);
		return contribution;
	}

	/**
	 * Executes the specified action with logging suppressed.
	 * @param action the action to be executed
	 */
	private void withLoggingSuppressed(Runnable action) {
		TurboFilterList turboFilters = getLoggerContext().getTurboFilterList();
		turboFilters.add(FILTER);
		try {
			action.run();
		}
		finally {
			turboFilters.remove(FILTER);
		}
	}

	/**
	 * {@link LoggingSystemFactory} that returns {@link LogbackLoggingSystem} if possible.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class Factory implements LoggingSystemFactory {

		private static final boolean PRESENT = ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext",
				Factory.class.getClassLoader());

		/**
		 * Returns the logging system based on the specified class loader.
		 * @param classLoader the class loader to be used for loading the logging system
		 * @return the logging system if it is present, otherwise null
		 */
		@Override
		public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
			if (PRESENT) {
				return new LogbackLoggingSystem(classLoader);
			}
			return null;
		}

	}

}
