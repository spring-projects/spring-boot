/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import org.springframework.boot.logging.LoggingSystem;

/**
 * Spring Boot {@link ConfigurationFactory} that prevents logger warnings from being
 * printed when the application first starts. This factory is ordered last and is
 * triggered by a {@code log4j2.springboot} classpath resource (which is bundled in this
 * jar). If the {@link Log4J2LoggingSystem} is active, a {@link DefaultConfiguration} is
 * returned with the expectation that the system will later re-initialize Log4J2 with the
 * correct configuration file.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
@Plugin(name = "SpringBootConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(0)
public class SpringBootConfigurationFactory extends ConfigurationFactory {

	private static final String[] TYPES = { ".springboot" };

	@Override
	protected String[] getSupportedTypes() {
		return TYPES;
	}

	@Override
	public Configuration getConfiguration(LoggerContext loggerContext,
			ConfigurationSource source) {
		if (source != null && source != ConfigurationSource.NULL_SOURCE) {
			if (LoggingSystem.get(loggerContext.getClass().getClassLoader()) != null) {
				return new DefaultConfiguration();
			}
		}
		return null;
	}

}
