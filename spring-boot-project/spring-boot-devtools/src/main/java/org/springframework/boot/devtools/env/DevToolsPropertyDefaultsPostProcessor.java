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

package org.springframework.boot.devtools.env;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.logger.DevToolsLogFactory;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.devtools.system.DevToolsEnablementDeducer;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to add properties that make sense when working at
 * development time.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.3.0
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DevToolsPropertyDefaultsPostProcessor implements EnvironmentPostProcessor {

	private static final Log logger = DevToolsLogFactory.getLog(DevToolsPropertyDefaultsPostProcessor.class);

	private static final String ENABLED = "spring.devtools.add-properties";

	private static final String WEB_LOGGING = "logging.level.web";

	private static final String[] WEB_ENVIRONMENT_CLASSES = {
			"org.springframework.web.context.ConfigurableWebEnvironment",
			"org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment" };

	private static final Map<String, Object> PROPERTIES;

	static {
		if (NativeDetector.inNativeImage()) {
			PROPERTIES = Collections.emptyMap();
		}
		else {
			PROPERTIES = loadDefaultProperties();
		}
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (DevToolsEnablementDeducer.shouldEnable(Thread.currentThread()) && isLocalApplication(environment)) {
			if (canAddProperties(environment)) {
				logger.info(LogMessage.format("Devtools property defaults active! Set '%s' to 'false' to disable",
						ENABLED));
				environment.getPropertySources().addLast(new MapPropertySource("devtools", PROPERTIES));
			}
			if (isWebApplication(environment) && !environment.containsProperty(WEB_LOGGING)) {
				logger.info(LogMessage.format(
						"For additional web related logging consider setting the '%s' property to 'DEBUG'",
						WEB_LOGGING));
			}
		}
	}

	private boolean isLocalApplication(ConfigurableEnvironment environment) {
		return environment.getPropertySources().get("remoteUrl") == null;
	}

	private boolean canAddProperties(Environment environment) {
		if (environment.getProperty(ENABLED, Boolean.class, true)) {
			return isRestarterInitialized() || isRemoteRestartEnabled(environment);
		}
		return false;
	}

	private boolean isRestarterInitialized() {
		try {
			Restarter restarter = Restarter.getInstance();
			return (restarter != null && restarter.getInitialUrls() != null);
		}
		catch (Exception ex) {
			return false;
		}
	}

	private boolean isRemoteRestartEnabled(Environment environment) {
		return environment.containsProperty("spring.devtools.remote.secret");
	}

	private boolean isWebApplication(Environment environment) {
		for (String candidate : WEB_ENVIRONMENT_CLASSES) {
			Class<?> environmentClass = resolveClassName(candidate, environment.getClass().getClassLoader());
			if (environmentClass != null && environmentClass.isInstance(environment)) {
				return true;
			}
		}
		return false;
	}

	private Class<?> resolveClassName(String candidate, ClassLoader classLoader) {
		try {
			return ClassUtils.resolveClassName(candidate, classLoader);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static Map<String, Object> loadDefaultProperties() {
		Properties properties = new Properties();
		try (InputStream stream = DevToolsPropertyDefaultsPostProcessor.class
			.getResourceAsStream("devtools-property-defaults.properties")) {
			if (stream == null) {
				throw new RuntimeException(
						"Failed to load devtools-property-defaults.properties because it doesn't exist");
			}
			properties.load(stream);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to load devtools-property-defaults.properties", ex);
		}
		Map<String, Object> map = new HashMap<>();
		for (String name : properties.stringPropertyNames()) {
			map.put(name, properties.getProperty(name));
		}
		return Collections.unmodifiableMap(map);
	}

}
