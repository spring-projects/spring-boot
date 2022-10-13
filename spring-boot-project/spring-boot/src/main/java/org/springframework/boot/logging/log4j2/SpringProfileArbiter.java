/*
 * Copyright 2012-2022 the original author or authors.
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
 * @since 3.0.0
 */
@Plugin(name = "SpringProfile", category = Node.CATEGORY, elementType = Arbiter.ELEMENT_TYPE, deferChildren = true,
		printObject = true)
public final class SpringProfileArbiter implements Arbiter {

	private final String[] profileNames;

	private final Environment environment;

	private SpringProfileArbiter(final String[] profiles, Environment environment) {
		this.profileNames = profiles;
		this.environment = environment;
	}

	@Override
	public boolean isCondition() {
		if (this.environment == null) {
			return false;
		}

		if (this.profileNames.length == 0) {
			return false;
		}
		return this.environment.acceptsProfiles(Profiles.of(this.profileNames));
	}

	@PluginBuilderFactory
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Standard Builder to create the Arbiter.
	 */
	public static class Builder implements org.apache.logging.log4j.core.util.Builder<SpringProfileArbiter> {

		private static final Logger LOGGER = StatusLogger.getLogger();

		/**
		 * Attribute name identifier.
		 */
		public static final String ATTR_NAME = "name";

		@PluginBuilderAttribute(ATTR_NAME)
		private String name;

		@PluginConfiguration
		private Configuration configuration;

		@PluginLoggerContext
		private LoggerContext loggerContext;

		/**
		 * Sets the Profile Name or Names.
		 * @param name the profile name(s).
		 * @return this
		 */
		public Builder setName(final String name) {
			this.name = name;
			return asBuilder();
		}

		public Builder setConfiguration(final Configuration configuration) {
			this.configuration = configuration;
			return asBuilder();
		}

		public Builder setLoggerContext(final LoggerContext loggerContext) {
			this.loggerContext = loggerContext;
			return asBuilder();
		}

		private SpringProfileArbiter.Builder asBuilder() {
			return this;
		}

		public SpringProfileArbiter build() {
			String[] profileNames = StringUtils.trimArrayElements(StringUtils
					.commaDelimitedListToStringArray(this.configuration.getStrSubstitutor().replace(this.name)));
			Environment environment = null;
			if (this.loggerContext != null) {
				environment = (Environment) this.loggerContext.getObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
				if (environment == null) {
					LOGGER.warn("Cannot create Arbiter, no Spring Environment provided");
					return null;
				}

				return new SpringProfileArbiter(profileNames, environment);
			}
			else {
				LOGGER.warn("Cannot create Arbiter, LoggerContext is not available");
			}
			return null;
		}

	}

}
