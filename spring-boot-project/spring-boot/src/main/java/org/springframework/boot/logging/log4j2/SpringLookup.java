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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerContextAware;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.springframework.core.env.Environment;

/**
 * Lookup for Spring properties.
 */
@Plugin(name = "spring", category = StrLookup.CATEGORY)
public class SpringLookup implements LoggerContextAware, StrLookup {

	private static final Logger LOGGER = StatusLogger.getLogger();

	private static final String ACTIVE = "profiles.active";

	private static final String DEFAULT = "profiles.default";

	private static final String PATTERN = "\\[(\\d+?)\\]";

	private static final Pattern ACTIVE_PATTERN = Pattern.compile(ACTIVE + PATTERN);

	private static final Pattern DEFAULT_PATTERN = Pattern.compile(DEFAULT + PATTERN);

	private volatile Environment environment;

	@Override
	public String lookup(String key) {
		if (environment != null) {
			String lowerKey = key.toLowerCase();
			if (lowerKey.startsWith(ACTIVE)) {
				switch (environment.getActiveProfiles().length) {
					case 0: {
						return null;
					}
					case 1: {
						return environment.getActiveProfiles()[0];
					}
					default: {
						Matcher matcher = ACTIVE_PATTERN.matcher(key);
						if (matcher.matches()) {
							try {
								int index = Integers.parseInt(matcher.group(1));
								if (index < environment.getActiveProfiles().length) {
									return environment.getActiveProfiles()[index];
								}
								LOGGER.warn("Index out of bounds for Spring active profiles: {}", index);
								return null;
							}
							catch (Exception ex) {
								LOGGER.warn("Unable to parse {} as integer value", matcher.group(1));
								return null;
							}

						}
						return String.join(",", environment.getActiveProfiles());
					}
				}
			}
			else if (lowerKey.startsWith(DEFAULT)) {
				switch (environment.getDefaultProfiles().length) {
					case 0: {
						return null;
					}
					case 1: {
						return environment.getDefaultProfiles()[0];
					}
					default: {
						Matcher matcher = DEFAULT_PATTERN.matcher(key);
						if (matcher.matches()) {
							try {
								int index = Integer.parseInt(matcher.group(1));
								if (index < environment.getDefaultProfiles().length) {
									return environment.getDefaultProfiles()[index];
								}
								LOGGER.warn("Index out of bounds for Spring default profiles: {}", index);
								return null;
							}
							catch (Exception ex) {
								LOGGER.warn("Unable to parse {} as integer value", matcher.group(1));
								return null;
							}

						}
						return String.join(",", environment.getDefaultProfiles());
					}
				}
			}

			return environment.getProperty(key);

		}
		return null;
	}

	@Override
	public String lookup(LogEvent event, String key) {
		return lookup((key));
	}

	@Override
	public void setLoggerContext(final LoggerContext loggerContext) {
		if (loggerContext != null) {
			environment = (Environment) loggerContext.getObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
		}
		else {
			LOGGER.warn("Attempt to set LoggerContext reference to null in SpringLookup");
		}
	}

}
