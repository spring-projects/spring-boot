/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
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
 * An Aribter that uses the active Spring profile to determine if configuration should be
 * included.
 */
@Plugin(name = "SpringProfile", category = Node.CATEGORY, elementType = Arbiter.ELEMENT_TYPE, deferChildren = true,
		printObject = true)
public class SpringProfileArbiter implements Arbiter {

	private final String[] profileNames;

	private final Environment environment;

	private SpringProfileArbiter(final String[] profiles, Environment environment) {
		this.profileNames = profiles;
		this.environment = environment;
	}

	@Override
	public boolean isCondition() {
		if (environment == null) {
			return false;
		}

		if (profileNames.length == 0) {
			return false;
		}
		return environment.acceptsProfiles(Profiles.of(profileNames));
	}

	@PluginBuilderFactory
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder implements org.apache.logging.log4j.core.util.Builder<SpringProfileArbiter> {

		private final Logger LOGGER = StatusLogger.getLogger();

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
			String[] profileNames = StringUtils.trimArrayElements(
					StringUtils.commaDelimitedListToStringArray(configuration.getStrSubstitutor().replace(name)));
			Environment environment = null;
			if (loggerContext != null) {
				environment = (Environment) loggerContext.getObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
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
