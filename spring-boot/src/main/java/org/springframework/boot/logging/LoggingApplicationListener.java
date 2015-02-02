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

package org.springframework.boot.logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationListener} that configures a logging framework depending on what it
 * finds on the classpath and in the {@link Environment}. If the environment contains a
 * property <code>logging.config</code> then that will be used to initialize the logging
 * system, otherwise a default location is used. The classpath is probed for log4j and
 * logback and if those are present they will be reconfigured, otherwise vanilla
 * <code>java.util.logging</code> will be used. </p>
 * <p>
 * The default config locations are <code>classpath:log4j.properties</code> or
 * <code>classpath:log4j.xml</code> for log4j; <code>classpath:logback.xml</code> for
 * logback; and <code>classpath:logging.properties</code> for
 * <code>java.util.logging</code>. If the correct one of those files is not found then
 * some sensible defaults are adopted from files of the same name but in the package
 * containing {@link LoggingApplicationListener}.
 * <p>
 * Some system properties may be set as side effects, and these can be useful if the
 * logging configuration supports placeholders (i.e. log4j or logback):
 * <ul>
 * <li><code>LOG_FILE</code> is set to the value of <code>logging.file</code> if found in
 * the environment</li>
 * <li><code>LOG_PATH</code> is set to the value of <code>logging.path</code> if found in
 * the environment</li>
 * <li><code>PID</code> is set to the value of the current process ID if it can be
 * determined</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class LoggingApplicationListener implements SmartApplicationListener {

	private static final Map<String, String> ENVIRONMENT_SYSTEM_PROPERTY_MAPPING;

	/**
	 * The name of the Spring property that contains a reference to the logging
	 * configuration to load.
	 */
	public static final String CONFIG_PROPERTY = "logging.config";

	/**
	 * The name of the Spring property that contains the path where the logging
	 * configuration can be found.
	 */
	public static final String PATH_PROPERTY = "logging.path";

	/**
	 * The name of the Spring property that contains the name of the logging configuration
	 * file.
	 */
	public static final String FILE_PROPERTY = "logging.file";

	/**
	 * The name of the System property that contains the process ID.
	 */
	public static final String PID_KEY = "PID";

	static {
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING = new HashMap<String, String>();
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put(FILE_PROPERTY, "LOG_FILE");
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put(PATH_PROPERTY, "LOG_PATH");
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("PID", PID_KEY);
	}

	private static MultiValueMap<LogLevel, String> LOG_LEVEL_LOGGERS;
	static {
		LOG_LEVEL_LOGGERS = new LinkedMultiValueMap<LogLevel, String>();
		LOG_LEVEL_LOGGERS.add(LogLevel.DEBUG, "org.springframework.boot");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.springframework");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.apache.tomcat");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.apache.catalina");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.eclipse.jetty");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.hibernate.tool.hbm2ddl");
	}

	private static Class<?>[] EVENT_TYPES = { ApplicationStartedEvent.class,
			ApplicationEnvironmentPreparedEvent.class, ContextClosedEvent.class };

	private static Class<?>[] SOURCE_TYPES = { SpringApplication.class,
			ApplicationContext.class };

	private final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.HIGHEST_PRECEDENCE + 11;

	private boolean parseArgs = true;

	private LogLevel springBootLogging = null;

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return isAssignableFrom(eventType, EVENT_TYPES);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return isAssignableFrom(sourceType, SOURCE_TYPES);
	}

	private boolean isAssignableFrom(Class<?> type, Class<?>... supportedTypes) {
		for (Class<?> supportedType : supportedTypes) {
			if (supportedType.isAssignableFrom(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent((ApplicationEnvironmentPreparedEvent) event);
		}
		else if (event instanceof ApplicationStartedEvent) {
			onApplicationStartedEvent((ApplicationStartedEvent) event);
		}
		else if (event instanceof ContextClosedEvent) {
			onContextClosedEvent((ContextClosedEvent) event);
		}
	}

	private void onApplicationEnvironmentPreparedEvent(
			ApplicationEnvironmentPreparedEvent event) {
		initialize(event.getEnvironment(), event.getSpringApplication().getClassLoader());
	}

	private void onApplicationStartedEvent(ApplicationStartedEvent event) {
		if (System.getProperty(PID_KEY) == null) {
			System.setProperty(PID_KEY, new ApplicationPid().toString());
		}
		LoggingSystem.get(ClassUtils.getDefaultClassLoader()).beforeInitialize();
	}

	private void onContextClosedEvent(ContextClosedEvent event) {
		LoggingSystem.get(ClassUtils.getDefaultClassLoader()).cleanUp();
	}

	/**
	 * Initialize the logging system according to preferences expressed through the
	 * {@link Environment} and the classpath.
	 */
	protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {
		initializeEarlyLoggingLevel(environment);
		cleanLogTempProperty();
		LoggingSystem system = LoggingSystem.get(classLoader);
		boolean systemEnvironmentChanged = mapSystemPropertiesFromSpring(environment);
		if (systemEnvironmentChanged) {
			// Re-initialize the defaults in case the system Environment changed
			system.beforeInitialize();
		}
		initializeSystem(environment, system);
		initializeFinalLoggingLevels(environment, system);
	}

	private void initializeEarlyLoggingLevel(ConfigurableEnvironment environment) {
		if (this.parseArgs && this.springBootLogging == null) {
			if (environment.containsProperty("debug")) {
				this.springBootLogging = LogLevel.DEBUG;
			}
			if (environment.containsProperty("trace")) {
				this.springBootLogging = LogLevel.TRACE;
			}
		}
	}

	private void cleanLogTempProperty() {
		// Logback won't read backslashes so add a clean path for it to use
		if (!StringUtils.hasLength(System.getProperty("LOG_TEMP"))) {
			String path = System.getProperty("java.io.tmpdir");
			if (path != null) {
				path = StringUtils.cleanPath(path);
				if (path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}
				System.setProperty("LOG_TEMP", path);
			}
		}
	}

	private boolean mapSystemPropertiesFromSpring(Environment environment) {
		boolean changed = false;
		for (Map.Entry<String, String> mapping : ENVIRONMENT_SYSTEM_PROPERTY_MAPPING
				.entrySet()) {
			String springName = mapping.getKey();
			String systemName = mapping.getValue();
			if (environment.containsProperty(springName)) {
				System.setProperty(systemName, environment.getProperty(springName));
				changed = true;
			}
		}
		return changed;
	}

	private void initializeSystem(ConfigurableEnvironment environment,
			LoggingSystem system) {
		if (environment.containsProperty(CONFIG_PROPERTY)) {
			String value = environment.getProperty(CONFIG_PROPERTY);
			try {
				ResourceUtils.getURL(value).openStream().close();
				system.initialize(value);
			}
			catch (Exception ex) {
				this.logger.warn("Logging environment value '" + value
						+ "' cannot be opened and will be ignored "
						+ "(using default location instead)");
				system.initialize();
			}
		}
		else {
			system.initialize();
		}
	}

	private void initializeFinalLoggingLevels(ConfigurableEnvironment environment,
			LoggingSystem system) {
		if (this.springBootLogging != null) {
			initializeLogLevel(system, this.springBootLogging);
		}
		setLogLevels(system, environment);
	}

	public void setLogLevels(LoggingSystem system, Environment environment) {
		Map<String, Object> levels = new RelaxedPropertyResolver(environment)
				.getSubProperties("logging.level.");
		for (Entry<String, Object> entry : levels.entrySet()) {
			setLogLevel(system, environment, entry.getKey(), entry.getValue().toString());
		}
	}

	private void setLogLevel(LoggingSystem system, Environment environment, String name,
			String level) {
		try {
			if (name.equalsIgnoreCase("root")) {
				name = null;
			}
			level = environment.resolvePlaceholders(level);
			system.setLogLevel(name, LogLevel.valueOf(level));
		}
		catch (RuntimeException ex) {
			this.logger.error("Cannot set level: " + level + " for '" + name + "'");
		}
	}

	protected void initializeLogLevel(LoggingSystem system, LogLevel level) {
		List<String> loggers = LOG_LEVEL_LOGGERS.get(level);
		if (loggers != null) {
			for (String logger : loggers) {
				system.setLogLevel(logger, level);
			}
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

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
	 * Sets if initialization arguments should be parsed for {@literal --debug} and
	 * {@literal --trace} options. Defaults to {@code true}.
	 * @param parseArgs if arguments should be parsed
	 */
	public void setParseArgs(boolean parseArgs) {
		this.parseArgs = parseArgs;
	}

}
