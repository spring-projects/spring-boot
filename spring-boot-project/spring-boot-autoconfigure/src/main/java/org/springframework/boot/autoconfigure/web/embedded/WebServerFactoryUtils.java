/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Utils for WebServerFactory.
 *
 * @author Yan Na
 * @since 2.1.0
 */
public final class WebServerFactoryUtils {
	private static final Logger logger = LoggerFactory
			.getLogger(WebServerFactoryUtils.class);

	private WebServerFactoryUtils() {

	}

	public static void coverEnvironmentServerPort(Environment environment,
			ServerProperties serverProperties) {
		if (environment == null) {
			logger.error("environment is null! exit coverEnvironmentServerPort()");
			return;
		}
		if (serverProperties == null) {
			logger.error("serverProperties is null! exit coverEnvironmentServerPort()");
			return;

		}
		boolean isSucceed = false;
		MutablePropertySources propertySourcesList = ((ConfigurableEnvironment) environment)
				.getPropertySources();
		if (propertySourcesList != null) {
			for (PropertySource<?> propertySource : propertySourcesList) {
				if (propertySource != null
						&& propertySource.containsProperty("server.port")) {
					Object source = propertySource.getSource();
					LinkedHashMap<String, String> sourceMap = null;
					if (source instanceof LinkedHashMap) {
						sourceMap = (LinkedHashMap<String, String>) source;
					}
					if (sourceMap != null && sourceMap.size() > 0) {
						Integer port = serverProperties.getPort();
						if (port != null && port > 0) {
							sourceMap.put("server.port", String.valueOf(port));
							isSucceed = true;
						}
					}
				}
			}
		}
		else {
			logger.error(
					"propertySourcesList is null! exit coverEnvironmentServerPort()");
			return;
		}
		if (isSucceed) {
			logger.info("Environment Port is " + serverProperties.getPort());
		}
		else {
			logger.error("server.port cover failed");
		}
	}
}
