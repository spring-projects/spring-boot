/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.context.logging;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerGroup;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationListener} that configures the {@link LoggingSystem}. If the
 * environment contains a {@code logging.config} property it will be used to bootstrap the
 * logging system, otherwise a default configuration is used. Regardless, logging levels
 * will be customized if the environment contains {@code logging.level.*} entries and
 * logging groups can be defined with {@code logging.group}.
 * <p>
 * Debug and trace logging for Spring, Tomcat, Jetty and Hibernate will be enabled when
 * the environment contains {@code debug} or {@code trace} properties that aren't set to
 * {@code "false"} (i.e. if you start your application using
 * {@literal java -jar myapp.jar [--debug | --trace]}). If you prefer to ignore these
 * properties you can set {@link #setParseArgs(boolean) parseArgs} to {@code false}.
 * <p>
 * By default, log output is only written to the console. If a log file is required, the
 * {@code logging.file.path} and {@code logging.file.name} properties can be used.
 * <p>
 * Some system properties may be set as side effects, and these can be useful if the
 * logging configuration supports placeholders (i.e. log4j or logback):
 * <ul>
 * <li>{@code LOG_FILE} is set to the value of path of the log file that should be written
 * (if any).</li>
 * <li>{@code PID} is set to the value of the current process ID if it can be determined.
 * </li>
 * </ul>
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author HaiTao Zhang
 * @since 2.0.0
 * @see LoggingSystem#get(ClassLoader)
 */
public class LoggingApplicationListener implements GenericApplicationListener {

	private static final ConfigurationPropertyName LOGGING_LEVEL = ConfigurationPropertyName.of("logging.level");

	private static final ConfigurationPropertyName LOGGING_GROUP = ConfigurationPropertyName.of("logging.group");

	private static final Bindable<Map<String, LogLevel>> STRING_LOGLEVEL_MAP = Bindable.mapOf(String.class,
			LogLevel.class);

	private static final Bindable<Map<String, List<String>>> STRING_STRINGS_MAP = Bindable
		.of(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class).asMap());

	/**
	 * The default order for the LoggingApplicationListener.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

	/**
	 * The name of the Spring property that contains a reference to the logging
	 * configuration to load.
	 */
	public static final String CONFIG_PROPERTY = "logging.config";

	/**
	 * The name of the Spring property that controls the registration of a shutdown hook
	 * to shut down the logging system when the JVM exits.
	 * @see LoggingSystem#getShutdownHandler
	 */
	public static final String REGISTER_SHUTDOWN_HOOK_PROPERTY = "logging.register-shutdown-hook";

	/**
	 * The name of the {@link LoggingSystem} bean.
	 */
	public static final String LOGGING_SYSTEM_BEAN_NAME = "springBootLoggingSystem";

	/**
	 * The name of the {@link LogFile} bean.
	 * @since 2.2.0
	 */
	public static final String LOG_FILE_BEAN_NAME = "springBootLogFile";

	/**
	 * The name of the {@link LoggerGroups} bean.
	 * @since 2.2.0
	 */
	public static final String LOGGER_GROUPS_BEAN_NAME = "springBootLoggerGroups";

	/**
	 * The name of the {@link Lifecycle} bean used to handle cleanup.
	 */
	private static final String LOGGING_LIFECYCLE_BEAN_NAME = "springBootLoggingLifecycle";

	private static final Map<String, List<String>> DEFAULT_GROUP_LOGGERS;
	static {
		MultiValueMap<String, String> loggers = new LinkedMultiValueMap<>();
		loggers.add("web", "org.springframework.core.codec");
		loggers.add("web", "org.springframework.http");
		loggers.add("web", "org.springframework.web");
		loggers.add("web", "org.springframework.boot.actuate.endpoint.web");
		loggers.add("web", "org.springframework.boot.web.servlet.ServletContextInitializerBeans");
		loggers.add("sql", "org.springframework.jdbc.core");
		loggers.add("sql", "org.hibernate.SQL");
		loggers.add("sql", "org.jooq.tools.LoggerListener");
		DEFAULT_GROUP_LOGGERS = Collections.unmodifiableMap(loggers);
	}

	private static final Map<LogLevel, List<String>> SPRING_BOOT_LOGGING_LOGGERS;
	static {
		MultiValueMap<LogLevel, String> loggers = new LinkedMultiValueMap<>();
		loggers.add(LogLevel.DEBUG, "sql");
		loggers.add(LogLevel.DEBUG, "web");
		loggers.add(LogLevel.DEBUG, "org.springframework.boot");
		loggers.add(LogLevel.TRACE, "org.springframework");
		loggers.add(LogLevel.TRACE, "org.apache.tomcat");
		loggers.add(LogLevel.TRACE, "org.apache.catalina");
		loggers.add(LogLevel.TRACE, "org.eclipse.jetty");
		loggers.add(LogLevel.TRACE, "org.hibernate.tool.hbm2ddl");
		SPRING_BOOT_LOGGING_LOGGERS = Collections.unmodifiableMap(loggers);
	}

	private static final Class<?>[] EVENT_TYPES = { ApplicationStartingEvent.class,
			ApplicationEnvironmentPreparedEvent.class, ApplicationPreparedEvent.class, ContextClosedEvent.class,
			ApplicationFailedEvent.class };

	private static final Class<?>[] SOURCE_TYPES = { SpringApplication.class, ApplicationContext.class };

	private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean();

	private final Log logger = LogFactory.getLog(getClass());

	private LoggingSystem loggingSystem;

	private LogFile logFile;

	private LoggerGroups loggerGroups;

	private int order = DEFAULT_ORDER;

	private boolean parseArgs = true;

	private LogLevel springBootLogging = null;

	/**
	 * Determines whether the specified event type is supported by this listener.
	 * @param resolvableType the event type to check
	 * @return true if the event type is supported, false otherwise
	 */
	@Override
	public boolean supportsEventType(ResolvableType resolvableType) {
		return isAssignableFrom(resolvableType.getRawClass(), EVENT_TYPES);
	}

	/**
	 * Determine if the specified source type is supported by this listener.
	 * @param sourceType the source type to check
	 * @return true if the source type is supported, false otherwise
	 */
	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return isAssignableFrom(sourceType, SOURCE_TYPES);
	}

	/**
	 * Checks if the given type is assignable from any of the supported types.
	 * @param type the type to check
	 * @param supportedTypes the supported types to compare against
	 * @return true if the given type is assignable from any of the supported types, false
	 * otherwise
	 */
	private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
		if (type != null) {
			for (Class<?> supportedType : supportedTypes) {
				if (supportedType.isAssignableFrom(type)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method is called when an application event is triggered. It handles different
	 * types of application events and performs specific actions accordingly.
	 * @param event The application event that is triggered
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartingEvent startingEvent) {
			onApplicationStartingEvent(startingEvent);
		}
		else if (event instanceof ApplicationEnvironmentPreparedEvent environmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent(environmentPreparedEvent);
		}
		else if (event instanceof ApplicationPreparedEvent preparedEvent) {
			onApplicationPreparedEvent(preparedEvent);
		}
		else if (event instanceof ContextClosedEvent) {
			onContextClosedEvent((ContextClosedEvent) event);
		}
		else if (event instanceof ApplicationFailedEvent) {
			onApplicationFailedEvent();
		}
	}

	/**
	 * This method is called when the application is starting. It initializes the logging
	 * system by getting the logging system instance using the class loader of the Spring
	 * application. It then calls the beforeInitialize() method of the logging system to
	 * perform any necessary initialization steps.
	 * @param event the ApplicationStartingEvent object representing the application
	 * starting event
	 */
	private void onApplicationStartingEvent(ApplicationStartingEvent event) {
		this.loggingSystem = LoggingSystem.get(event.getSpringApplication().getClassLoader());
		this.loggingSystem.beforeInitialize();
	}

	/**
	 * This method is called when the ApplicationEnvironmentPreparedEvent is triggered. It
	 * initializes the logging system and sets the logging system to the current class
	 * loader.
	 * @param event The ApplicationEnvironmentPreparedEvent object containing the event
	 * details.
	 */
	private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		SpringApplication springApplication = event.getSpringApplication();
		if (this.loggingSystem == null) {
			this.loggingSystem = LoggingSystem.get(springApplication.getClassLoader());
		}
		initialize(event.getEnvironment(), springApplication.getClassLoader());
	}

	/**
	 * This method is called when the application is prepared. It registers necessary
	 * beans related to logging in the application context.
	 * @param event The ApplicationPreparedEvent object representing the event.
	 */
	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		ConfigurableApplicationContext applicationContext = event.getApplicationContext();
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		if (!beanFactory.containsBean(LOGGING_SYSTEM_BEAN_NAME)) {
			beanFactory.registerSingleton(LOGGING_SYSTEM_BEAN_NAME, this.loggingSystem);
		}
		if (this.logFile != null && !beanFactory.containsBean(LOG_FILE_BEAN_NAME)) {
			beanFactory.registerSingleton(LOG_FILE_BEAN_NAME, this.logFile);
		}
		if (this.loggerGroups != null && !beanFactory.containsBean(LOGGER_GROUPS_BEAN_NAME)) {
			beanFactory.registerSingleton(LOGGER_GROUPS_BEAN_NAME, this.loggerGroups);
		}
		if (!beanFactory.containsBean(LOGGING_LIFECYCLE_BEAN_NAME) && applicationContext.getParent() == null) {
			beanFactory.registerSingleton(LOGGING_LIFECYCLE_BEAN_NAME, new Lifecycle());
		}
	}

	/**
	 * This method is called when the context is closed. It checks if the application
	 * context has a parent or if it contains a bean with the name
	 * LOGGING_LIFECYCLE_BEAN_NAME. If neither of these conditions are met, it proceeds to
	 * clean up the logging system.
	 * @param event The ContextClosedEvent that triggered this method.
	 */
	private void onContextClosedEvent(ContextClosedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		if (applicationContext.getParent() != null || applicationContext.containsBean(LOGGING_LIFECYCLE_BEAN_NAME)) {
			return;
		}
		cleanupLoggingSystem();
	}

	/**
	 * Cleans up the logging system.
	 *
	 * This method checks if the logging system is not null and then calls the cleanUp()
	 * method of the logging system.
	 *
	 * @see LoggingSystem#cleanUp()
	 */
	void cleanupLoggingSystem() {
		if (this.loggingSystem != null) {
			this.loggingSystem.cleanUp();
		}
	}

	/**
	 * Cleans up the logging system when the application fails.
	 */
	private void onApplicationFailedEvent() {
		cleanupLoggingSystem();
	}

	/**
	 * Initialize the logging system according to preferences expressed through the
	 * {@link Environment} and the classpath.
	 * @param environment the environment
	 * @param classLoader the classloader
	 */
	protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {
		getLoggingSystemProperties(environment).apply();
		this.logFile = LogFile.get(environment);
		if (this.logFile != null) {
			this.logFile.applyToSystemProperties();
		}
		this.loggerGroups = new LoggerGroups(DEFAULT_GROUP_LOGGERS);
		initializeEarlyLoggingLevel(environment);
		initializeSystem(environment, this.loggingSystem, this.logFile);
		initializeFinalLoggingLevels(environment, this.loggingSystem);
		registerShutdownHookIfNecessary(environment, this.loggingSystem);
	}

	/**
	 * Retrieves the logging system properties based on the provided environment. If a
	 * logging system is available, it delegates the retrieval to the logging system.
	 * Otherwise, it creates a new instance of LoggingSystemProperties using the
	 * environment.
	 * @param environment the configurable environment containing the properties
	 * @return the logging system properties
	 */
	private LoggingSystemProperties getLoggingSystemProperties(ConfigurableEnvironment environment) {
		return (this.loggingSystem != null) ? this.loggingSystem.getSystemProperties(environment)
				: new LoggingSystemProperties(environment);
	}

	/**
	 * Initializes the early logging level based on the environment configuration. If the
	 * parseArgs flag is set and the springBootLogging level is not already set, it checks
	 * if the "debug" or "trace" properties are set in the environment and sets the
	 * springBootLogging level accordingly.
	 * @param environment the ConfigurableEnvironment object representing the environment
	 * configuration
	 */
	private void initializeEarlyLoggingLevel(ConfigurableEnvironment environment) {
		if (this.parseArgs && this.springBootLogging == null) {
			if (isSet(environment, "debug")) {
				this.springBootLogging = LogLevel.DEBUG;
			}
			if (isSet(environment, "trace")) {
				this.springBootLogging = LogLevel.TRACE;
			}
		}
	}

	/**
	 * Checks if a property is set in the given ConfigurableEnvironment.
	 * @param environment the ConfigurableEnvironment to check the property in
	 * @param property the name of the property to check
	 * @return true if the property is set and its value is not "false", false otherwise
	 */
	private boolean isSet(ConfigurableEnvironment environment, String property) {
		String value = environment.getProperty(property);
		return (value != null && !value.equals("false"));
	}

	/**
	 * Initializes the system with the given environment, logging system, and log file.
	 * @param environment The configurable environment.
	 * @param system The logging system.
	 * @param logFile The log file.
	 */
	private void initializeSystem(ConfigurableEnvironment environment, LoggingSystem system, LogFile logFile) {
		String logConfig = environment.getProperty(CONFIG_PROPERTY);
		if (StringUtils.hasLength(logConfig)) {
			logConfig = logConfig.strip();
		}
		try {
			LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
			if (ignoreLogConfig(logConfig)) {
				system.initialize(initializationContext, null, logFile);
			}
			else {
				system.initialize(initializationContext, logConfig, logFile);
			}
		}
		catch (Throwable ex) {
			Throwable exceptionToReport = ex;
			while (exceptionToReport != null && !(exceptionToReport instanceof FileNotFoundException)) {
				exceptionToReport = exceptionToReport.getCause();
			}
			exceptionToReport = (exceptionToReport != null) ? exceptionToReport : ex;
			// NOTE: We can't use the logger here to report the problem
			System.err.println("Logging system failed to initialize using configuration from '" + logConfig + "'");
			exceptionToReport.printStackTrace(System.err);
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Checks if the given log configuration should be ignored.
	 * @param logConfig the log configuration to check
	 * @return {@code true} if the log configuration should be ignored, {@code false}
	 * otherwise
	 */
	private boolean ignoreLogConfig(String logConfig) {
		return !StringUtils.hasLength(logConfig) || logConfig.startsWith("-D");
	}

	/**
	 * Initializes the final logging levels based on the provided environment and logging
	 * system.
	 * @param environment the configurable environment containing the logging
	 * configuration
	 * @param system the logging system to be initialized
	 */
	private void initializeFinalLoggingLevels(ConfigurableEnvironment environment, LoggingSystem system) {
		bindLoggerGroups(environment);
		if (this.springBootLogging != null) {
			initializeSpringBootLogging(system, this.springBootLogging);
		}
		setLogLevels(system, environment);
	}

	/**
	 * Binds the logger groups to the environment.
	 * @param environment the configurable environment
	 */
	private void bindLoggerGroups(ConfigurableEnvironment environment) {
		if (this.loggerGroups != null) {
			Binder binder = Binder.get(environment);
			binder.bind(LOGGING_GROUP, STRING_STRINGS_MAP).ifBound(this.loggerGroups::putAll);
		}
	}

	/**
	 * Initialize loggers based on the {@link #setSpringBootLogging(LogLevel)
	 * springBootLogging} setting. By default this implementation will pick an appropriate
	 * set of loggers to configure based on the level.
	 * @param system the logging system
	 * @param springBootLogging the spring boot logging level requested
	 * @since 2.2.0
	 */
	protected void initializeSpringBootLogging(LoggingSystem system, LogLevel springBootLogging) {
		BiConsumer<String, LogLevel> configurer = getLogLevelConfigurer(system);
		SPRING_BOOT_LOGGING_LOGGERS.getOrDefault(springBootLogging, Collections.emptyList())
			.forEach((name) -> configureLogLevel(name, springBootLogging, configurer));
	}

	/**
	 * Set logging levels based on relevant {@link Environment} properties.
	 * @param system the logging system
	 * @param environment the environment
	 * @since 2.2.0
	 */
	protected void setLogLevels(LoggingSystem system, ConfigurableEnvironment environment) {
		BiConsumer<String, LogLevel> customizer = getLogLevelConfigurer(system);
		Binder binder = Binder.get(environment);
		Map<String, LogLevel> levels = binder.bind(LOGGING_LEVEL, STRING_LOGLEVEL_MAP).orElseGet(Collections::emptyMap);
		levels.forEach((name, level) -> configureLogLevel(name, level, customizer));
	}

	/**
	 * Configures the log level for a specific logger.
	 * @param name the name of the logger
	 * @param level the log level to be configured
	 * @param configurer the function to configure the log level
	 */
	private void configureLogLevel(String name, LogLevel level, BiConsumer<String, LogLevel> configurer) {
		if (this.loggerGroups != null) {
			LoggerGroup group = this.loggerGroups.get(name);
			if (group != null && group.hasMembers()) {
				group.configureLogLevel(level, configurer);
				return;
			}
		}
		configurer.accept(name, level);
	}

	/**
	 * Returns a BiConsumer that configures the log level for a given logger name using
	 * the specified LoggingSystem. The BiConsumer takes in a logger name and a log level,
	 * and sets the log level for the logger using the LoggingSystem. If the logger name
	 * is equal to the root logger name, it is set to null. If an exception occurs while
	 * setting the log level, an error log message is logged.
	 * @param system the LoggingSystem to use for configuring log levels
	 * @return a BiConsumer that configures the log level for a given logger name using
	 * the specified LoggingSystem
	 */
	private BiConsumer<String, LogLevel> getLogLevelConfigurer(LoggingSystem system) {
		return (name, level) -> {
			try {
				name = name.equalsIgnoreCase(LoggingSystem.ROOT_LOGGER_NAME) ? null : name;
				system.setLogLevel(name, level);
			}
			catch (RuntimeException ex) {
				this.logger.error(LogMessage.format("Cannot set level '%s' for '%s'", level, name));
			}
		};
	}

	/**
	 * Registers a shutdown hook if necessary.
	 * @param environment the environment
	 * @param loggingSystem the logging system
	 */
	private void registerShutdownHookIfNecessary(Environment environment, LoggingSystem loggingSystem) {
		if (environment.getProperty(REGISTER_SHUTDOWN_HOOK_PROPERTY, Boolean.class, true)) {
			Runnable shutdownHandler = loggingSystem.getShutdownHandler();
			if (shutdownHandler != null && shutdownHookRegistered.compareAndSet(false, true)) {
				registerShutdownHook(shutdownHandler);
			}
		}
	}

	/**
	 * Registers a shutdown hook to be executed when the application is shutting down.
	 * @param shutdownHandler the Runnable object to be executed on shutdown
	 */
	void registerShutdownHook(Runnable shutdownHandler) {
		SpringApplication.getShutdownHandlers().add(shutdownHandler);
	}

	/**
	 * Sets the order of the LoggingApplicationListener.
	 * @param order the order value to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns the order value of this LoggingApplicationListener.
	 * @return the order value of this LoggingApplicationListener
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets a custom logging level to be used for Spring Boot and related libraries.
	 * @param springBootLogging the logging level
	 */
	public void setSpringBootLogging(LogLevel springBootLogging) {
		this.springBootLogging = springBootLogging;
	}

	/**
	 * Sets if initialization arguments should be parsed for {@literal debug} and
	 * {@literal trace} properties (usually defined from {@literal --debug} or
	 * {@literal --trace} command line args). Defaults to {@code true}.
	 * @param parseArgs if arguments should be parsed
	 */
	public void setParseArgs(boolean parseArgs) {
		this.parseArgs = parseArgs;
	}

	/**
	 * Lifecycle class.
	 */
	private final class Lifecycle implements SmartLifecycle {

		private volatile boolean running;

		/**
		 * Starts the lifecycle.
		 */
		@Override
		public void start() {
			this.running = true;
		}

		/**
		 * Stops the lifecycle of the application. Sets the running flag to false,
		 * indicating that the application is no longer running. Cleans up the logging
		 * system.
		 */
		@Override
		public void stop() {
			this.running = false;
			cleanupLoggingSystem();
		}

		/**
		 * Returns a boolean value indicating whether the Lifecycle is currently running.
		 * @return true if the Lifecycle is running, false otherwise.
		 */
		@Override
		public boolean isRunning() {
			return this.running;
		}

		/**
		 * Returns the phase of the lifecycle.
		 *
		 * The phase of the lifecycle is determined by the order in which the lifecycles
		 * are executed. This method returns the phase of the lifecycle, which is set to
		 * be shutdown late and always after WebServerStartStopLifecycle.
		 * @return the phase of the lifecycle
		 */
		@Override
		public int getPhase() {
			// Shutdown late and always after WebServerStartStopLifecycle
			return Integer.MIN_VALUE + 1;
		}

	}

}
