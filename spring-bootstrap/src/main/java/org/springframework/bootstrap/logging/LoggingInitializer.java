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

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.Log4jConfigurer;

/**
 * <p>
 * An {@link ApplicationContextInitializer} that configures a logging framework depending
 * on what it finds on the classpath and in the {@link Environment}. If the environment
 * contains a property <code>logging.config</code> then that will be used to initialize
 * the logging system, otherwise a default location is used. The classpath is probed for
 * log4j and logback and if those are present they will be reconfigured, otherwise vanilla
 * <code>java.util.logging</code> will be used.
 * </p>
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
 * @author Dave Syer
 * 
 */
public class LoggingInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private int order = Integer.MIN_VALUE + 1;

	/**
	 * Initialize the logging system according to preferences expressed through the
	 * {@link Environment} and the classpath.
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {

		String configLocation = getDefaultConfigLocation();

		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		if (environment.containsProperty("logging.config")) {
			configLocation = environment.getProperty("logging.config");
		}

		if (environment.containsProperty("logging.file")) {
			String location = environment.getProperty("logging.file");
			System.setProperty("LOG_FILE", location);
		}

		if (environment.containsProperty("logging.path")) {
			String location = environment.getProperty("logging.path");
			System.setProperty("LOG_PATH", location);
		}

		if (!environment.containsProperty("PID")) {
			System.setProperty("PID", getPid());
		}

		initLogging(applicationContext, configLocation);

	}

	private void initLogging(ResourceLoader resourceLoader, String configLocation) {
		try {
			if (isLog4jPresent()) {
				Log4jConfigurer.initLogging(configLocation);
			} else {
				if (isLogbackPresent()) {
					LogbackConfigurer.initLogging(configLocation);
				} else {
					JavaLoggerConfigurer.initLogging(configLocation);
				}
			}
		} catch (RuntimeException e) {
			throw e;

		} catch (Exception e) {
			throw new IllegalStateException("Cannot initialize logging from "
					+ configLocation, e);
		}
	}

	private boolean isLogbackPresent() {
		return ClassUtils.isPresent("ch.qos.logback.core.Appender",
				ClassUtils.getDefaultClassLoader());
	}

	private boolean isLog4jPresent() {
		return ClassUtils.isPresent("org.apache.log4j.PropertyConfigurator",
				ClassUtils.getDefaultClassLoader());
	}

	private String getDefaultConfigLocation() {

		String defaultPath = ClassUtils.getPackageName(LoggingInitializer.class).replace(
				".", "/")
				+ "/";

		if (isLog4jPresent()) {
			String path = "log4j.xml";
			if (!new ClassPathResource(path).exists()) {
				path = "log4j.properties";
				if (!new ClassPathResource(path).exists()) {
					path = defaultPath + path;
				}
			}
			return "classpath:" + path;
		}

		if (isLogbackPresent()) {
			String path = "logback.xml";
			if (!new ClassPathResource(path).exists()) {
				path = defaultPath + path;
			}
			return "classpath:" + path;
		}

		String path = "logging.properties";
		if (!new ClassPathResource(path).exists()) {
			path = defaultPath + path;
		}

		return "classpath:" + path;

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
}
