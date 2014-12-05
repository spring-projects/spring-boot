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

package org.springframework.boot.logging;

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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
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
 * An {@link ApplicationListener} that configures the {@link LoggingSystem}. If the
 * environment contains a {@code logging.config} property a then that will be used to
 * initialize the logging system, otherwise a default configuration is used.
 * <p>
 * By default, log output is only written to the console. If a log file is required the
 * {@code logging.path} and {@code logging.file} properties can be used.
 * <p>
 * Some system properties may be set as side effects, and these can be useful if the
 * logging configuration supports placeholders (i.e. log4j or logback):
 * <ul>
 * <li>{@code LOG_FILE} is set to the value of path of the log file that should be written
 * (if any).</li>
 * <li>{@code PID} is set to the value of the current process ID if it can be determined.</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see LoggingSystem#get(ClassLoader)
 */
public class LoggingApplicationListener implements SmartApplicationListener {

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

	private static MultiValueMap<LogLevel, String> LOG_LEVEL_LOGGERS;
	static {
		LOG_LEVEL_LOGGERS = new LinkedMultiValueMap<LogLevel, String>();
		LOG_LEVEL_LOGGERS.add(LogLevel.DEBUG, "org.springframework.boot");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.springframework");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.apache.tomcat");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.apache.catalina");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.eclipse.jetty");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.hibernate.tool.hbm2ddl");
		LOG_LEVEL_LOGGERS.add(LogLevel.DEBUG, "org.hibernate.SQL");
	}

	private final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.HIGHEST_PRECEDENCE + 11;

	private boolean parseArgs = true;

	private LogLevel springBootLogging = null;

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationStartedEvent.class.isAssignableFrom(eventType)
				|| ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return SpringApplication.class.isAssignableFrom(sourceType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartedEvent) {
			onApplicationStartedEvent((ApplicationStartedEvent) event);
		}
		else if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationPreparedEvent((ApplicationEnvironmentPreparedEvent) event);
		}
	}

	private void onApplicationStartedEvent(ApplicationStartedEvent event) {
		LoggingSystem.get(ClassUtils.getDefaultClassLoader()).beforeInitialize();
	}

	private void onApplicationPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		initialize(event.getEnvironment(), event.getSpringApplication().getClassLoader());
	}

	/**
	 * Initialize the logging system according to preferences expressed through the
	 * {@link Environment} and the classpath.
	 */
	protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {
		if (System.getProperty(PID_KEY) == null) {
			System.setProperty(PID_KEY, new ApplicationPid().toString());
		}
		initializeEarlyLoggingLevel(environment);
		LoggingSystem system = LoggingSystem.get(classLoader);
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

	private void initializeSystem(ConfigurableEnvironment environment,
			LoggingSystem system) {
		String logFile = getLogFile(environment);
		String logConfig = environment.getProperty(CONFIG_PROPERTY);
		if (StringUtils.hasLength(logConfig)) {
			try {
				ResourceUtils.getURL(logConfig).openStream().close();
				system.initialize(logConfig, logFile);
			}
			catch (Exception ex) {
				this.logger.warn("Logging environment value '" + logConfig
						+ "' cannot be opened and will be ignored "
						+ "(using default location instead)");
				system.initialize(null, logFile);
			}
		}
		else {
			system.initialize(null, logFile);
		}
	}

	private String getLogFile(ConfigurableEnvironment environment) {
		String file = environment.getProperty(FILE_PROPERTY);
		String path = environment.getProperty(PATH_PROPERTY);
		if (StringUtils.hasLength(path) || StringUtils.hasLength(file)) {
			if (!StringUtils.hasLength(file)) {
				file = "spring.log";
			}
			if (!StringUtils.hasLength(path) && !file.contains("/")) {
				path = StringUtils.cleanPath(System.getProperty("java.io.tmpdir"));
			}
			if (StringUtils.hasLength(path)) {
				return StringUtils.applyRelativePath(path, file);
			}
			return file;
		}
		return null;
	}

	private void initializeFinalLoggingLevels(ConfigurableEnvironment environment,
			LoggingSystem system) {
		if (this.springBootLogging != null) {
			initializeLogLevel(system, this.springBootLogging);
		}
		setLogLevels(system, environment);
	}

	protected void initializeLogLevel(LoggingSystem system, LogLevel level) {
		List<String> loggers = LOG_LEVEL_LOGGERS.get(level);
		if (loggers != null) {
			for (String logger : loggers) {
				system.setLogLevel(logger, level);
			}
		}
	}

	protected void setLogLevels(LoggingSystem system, Environment environment) {
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
