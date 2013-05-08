/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.logging;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.Log4jConfigurer;

/**
 * An {@link ApplicationContextInitializer} that configures a logging framework depending
 * on what it finds on the classpath and in the {@link Environment}. If the environment
 * contains a property <code>logging.config</code> then that will be used to initialize
 * the logging system, otherwise a default location is used. The classpath is probed for
 * log4j and logback and if those are present they will be reconfigured, otherwise vanilla
 * <code>java.util.logging</code> will be used. </p>
 * 
 * <p>
 * The default config locations are <code>classpath:log4j.properties</code> or
 * <code>classpath:log4j.xml</code> for log4j; <code>classpath:logback.xml</code> for
 * logback; and <code>classpath:logging.properties</code> for
 * <code>java.util.logging</code>. If the correct one of those files is not found then
 * some sensible defaults are adopted from files of the same name but in the package
 * containing {@link LoggingInitializer}.
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
public class LoggingInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private static final Map<String, String> ENVIRONMENT_SYSTEM_PROPERTY_MAPPING;
	static {
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING = new HashMap<String, String>();
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("logging.file", "LOG_FILE");
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("logging.path", "LOG_PATH");
		ENVIRONMENT_SYSTEM_PROPERTY_MAPPING.put("PID", "PID");
	}

	private int order = Integer.MIN_VALUE + 1;

	/**
	 * Initialize the logging system according to preferences expressed through the
	 * {@link Environment} and the classpath.
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {

		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		for (Map.Entry<String, String> mapping : ENVIRONMENT_SYSTEM_PROPERTY_MAPPING
				.entrySet()) {
			if (environment.containsProperty(mapping.getKey())) {
				System.setProperty(mapping.getValue(),
						environment.getProperty(mapping.getKey()));
			}
		}

		if (System.getProperty("PID") == null) {
			System.setProperty("PID", getPid());
		}

		LoggingSystem system = LoggingSystem.get(applicationContext.getClassLoader());
		system.init(applicationContext);
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

	private static enum LoggingSystem {

		/**
		 * Log4J
		 */
		LOG4J("org.apache.log4j.PropertyConfigurator", "log4j.xml", "log4j.properties") {

			@Override
			protected void doInit(ApplicationContext applicationContext,
					String configLocation) throws Exception {
				Log4jConfigurer.initLogging(configLocation);
			}
		},

		/**
		 * Logback
		 */
		LOGBACK("ch.qos.logback.core.Appender", "logback.xml") {

			@Override
			protected void doInit(ApplicationContext applicationContext,
					String configLocation) throws Exception {
				LogbackConfigurer.initLogging(configLocation);
			}
		},

		/**
		 * Java Util Logging
		 */
		JAVA(null, "logging.properties") {

			@Override
			protected void doInit(ApplicationContext applicationContext,
					String configLocation) throws Exception {
				JavaLoggerConfigurer.initLogging(configLocation);
			}
		};

		private final String className;

		private final String[] paths;

		private LoggingSystem(String className, String... paths) {
			this.className = className;
			this.paths = paths;
		}

		public void init(ApplicationContext applicationContext) {
			String configLocation = getConfigLocation(applicationContext);
			try {
				doInit(applicationContext, configLocation);
			} catch (RuntimeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new IllegalStateException("Cannot initialize logging from "
						+ configLocation, ex);
			}

		}

		protected abstract void doInit(ApplicationContext applicationContext,
				String configLocation) throws Exception;

		private String getConfigLocation(ApplicationContext applicationContext) {
			Environment environment = applicationContext.getEnvironment();
			ClassLoader classLoader = applicationContext.getClassLoader();

			// User specified config
			if (environment.containsProperty("logging.config")) {
				return environment.getProperty("logging.config");
			}

			// Common patterns
			for (String path : this.paths) {
				ClassPathResource resource = new ClassPathResource(path, classLoader);
				if (resource.exists()) {
					return "classpath:" + path;
				}
			}

			// Fallback to the default
			String defaultPath = ClassUtils.getPackageName(LoggingInitializer.class);
			defaultPath = defaultPath.replace(".", "/");
			defaultPath = defaultPath + "/" + this.paths[this.paths.length - 1];
			return "classpath:" + defaultPath;
		}

		public static LoggingSystem get(ClassLoader classLoader) {
			for (LoggingSystem loggingSystem : values()) {
				String className = loggingSystem.className;
				if (className == null || ClassUtils.isPresent(className, classLoader)) {
					return loggingSystem;
				}
			}
			return JAVA;
		}

	}

}
