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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginLoggerContext;
import org.apache.logging.log4j.status.StatusLogger;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * An Arbiter that uses the active Spring profile to determine if configuration should be
 * included.
 *
 * @author Ralph Goers
 */
@Plugin(name = "SpringProfile", category = Node.CATEGORY, elementType = Arbiter.ELEMENT_TYPE, deferChildren = true,
		printObject = true)
final class SpringProfileArbiter implements Arbiter {

	private final Environment environment;

	private final Profiles profiles;

	/**
	 * Constructs a new SpringProfileArbiter with the specified environment and profiles.
	 * @param environment the environment used to determine the active profiles
	 * @param profiles an array of profiles to be considered
	 */
	private SpringProfileArbiter(Environment environment, String[] profiles) {
		this.environment = environment;
		this.profiles = Profiles.of(profiles);
	}

	/**
	 * Checks if the condition is met based on the environment and profiles.
	 * @return true if the condition is met, false otherwise
	 */
	@Override
	public boolean isCondition() {
		return (this.environment != null) && this.environment.acceptsProfiles(this.profiles);
	}

	/**
	 * Creates a new instance of the {@code Builder} class.
	 * @return a new instance of the {@code Builder} class
	 */
	@PluginBuilderFactory
	static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Standard Builder to create the Arbiter.
	 */
	static final class Builder implements org.apache.logging.log4j.core.util.Builder<SpringProfileArbiter> {

		private static final Logger statusLogger = StatusLogger.getLogger();

		@PluginBuilderAttribute
		private String name;

		@PluginConfiguration
		private Configuration configuration;

		@PluginLoggerContext
		private LoggerContext loggerContext;

		/**
		 * Private constructor for the Builder class.
		 */
		private Builder() {
		}

		/**
		 * Sets the profile name or expression.
		 * @param name the profile name or expression
		 * @return this
		 * @see Profiles#of(String...)
		 */
		Builder setName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Builds a SpringProfileArbiter object based on the provided configuration.
		 * @return the constructed SpringProfileArbiter object, or null if no Spring
		 * Environment is available
		 */
		@Override
		public SpringProfileArbiter build() {
			Environment environment = Log4J2LoggingSystem.getEnvironment(this.loggerContext);
			if (environment == null) {
				statusLogger.warn("Cannot create Arbiter, no Spring Environment available");
				return null;
			}
			String name = this.configuration.getStrSubstitutor().replace(this.name);
			String[] profiles = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
			return new SpringProfileArbiter(environment, profiles);
		}

	}

}
