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

package org.springframework.boot.context.listener;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationEnvironmentAvailableEvent;
import org.springframework.boot.SpringApplicationStartEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;

/**
 * An {@link ApplicationListener} that configures a logging framework depending on what it
 * finds on the classpath and in the {@link Environment}. If the environment contains a
 * property <code>logging.config</code> then that will be used to initialize the logging
 * system, otherwise a default location is used. The classpath is probed for log4j and
 * logback and if those are present they will be reconfigured, otherwise vanilla
 * <code>java.util.logging</code> will be used. </p>
 * 
 * <p>
 * The default config locations are <code>classpath:log4j.properties</code> or
 * <code>classpath:log4j.xml</code> for log4j; <code>classpath:logback.xml</code> for
 * logback; and <code>classpath:logging.properties</code> for
 * <code>java.util.logging</code>. If the correct one of those files is not found then
 * some sensible defaults are adopted from files of the same name but in the package
 * containing {@link LoggingApplicationListener}.
 * </p>
 * 
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
 */
public class LoggingApplicationListener implements SmartApplicationListener {

	private static final Map<String, String> ENVIRONMENT_SYSTEM_PROPERTY_MAPPING;
	static {
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING = new HashMap<String, String>();
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("logging.file", "LOG_FILE");
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("logging.path", "LOG_PATH");
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("PID", "PID");
	}

	private static MultiValueMap<LogLevel, String> LOG_LEVEL_LOGGERS;
	static {
		LOG_LEVEL_LOGGERS = new LinkedMultiValueMap<LogLevel, String>();
		LOG_LEVEL_LOGGERS.add(LogLevel.DEBUG, "org.springframework.boot");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.springframework");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.apache.tomcat");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.eclipse.jetty");
		LOG_LEVEL_LOGGERS.add(LogLevel.TRACE, "org.hibernate.tool.hbm2ddl");
	}

	@SuppressWarnings("unchecked")
	private static Collection<Class<? extends ApplicationEvent>> EVENT_TYPES = Arrays
			.<Class<? extends ApplicationEvent>> asList(
					SpringApplicationStartEvent.class,
					SpringApplicationEnvironmentAvailableEvent.class);

	private final Log logger = LogFactory.getLog(getClass());

	private int order = Integer.MIN_VALUE + 11;

	private boolean parseArgs = true;

	private LogLevel springBootLogging = null;

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		for (Class<? extends ApplicationEvent> type : EVENT_TYPES) {
			if (type.isAssignableFrom(eventType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return SpringApplication.class.isAssignableFrom(sourceType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof SpringApplicationEnvironmentAvailableEvent) {
			SpringApplicationEnvironmentAvailableEvent available = (SpringApplicationEnvironmentAvailableEvent) event;
			initialize(available.getEnvironment(), available.getSpringApplication()
					.getClassLoader());
		}
		else {
			if (System.getProperty("PID") == null) {
				System.setProperty("PID", getPid());
			}
			LoggingSystem loggingSystem = LoggingSystem.get(ClassUtils
					.getDefaultClassLoader());
			loggingSystem.beforeInitialize();
		}
	}

	/**
	 * Initialize the logging system according to preferences expressed through the
	 * {@link Environment} and the classpath.
	 */
	protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {

		if (this.parseArgs && this.springBootLogging == null) {
			if (environment.containsProperty("debug")) {
				this.springBootLogging = LogLevel.DEBUG;
			}
			if (environment.containsProperty("trace")) {
				this.springBootLogging = LogLevel.TRACE;
			}
		}

		boolean environmentChanged = false;
		for (Map.Entry<String, String> mapping : ENVIRONMENT_SYSTEM_PROPERTY_MAPPING
				.entrySet()) {
			if (environment.containsProperty(mapping.getKey())) {
				System.setProperty(mapping.getValue(),
						environment.getProperty(mapping.getKey()));
				environmentChanged = true;
			}
		}

		LoggingSystem system = LoggingSystem.get(classLoader);

		if (environmentChanged) {
			// Re-initialize the defaults in case the Environment changed
			system.beforeInitialize();
		}
		// User specified configuration
		if (environment.containsProperty("logging.config")) {
			String value = environment.getProperty("logging.config");
			try {
				ResourceUtils.getURL(value).openStream().close();
				system.initialize(value);
				return;
			}
			catch (Exception ex) {
				// Swallow exception and continue
			}
			this.logger.warn("Logging environment value '" + value
					+ "' cannot be opened and will be ignored");
		}

		system.initialize();
		if (this.springBootLogging != null) {
			initializeLogLevel(system, this.springBootLogging);
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

	private String getPid() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		if (name != null) {
			return name.split("@")[0];
		}
		return "????";
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
