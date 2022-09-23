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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerContextAware;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.status.StatusLogger;

import org.springframework.core.env.Environment;

/**
 * Lookup for Spring properties.
 *
 * @author Ralph Goers
 * @since 3.0.0
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
		if (this.environment == null) {
			return null;
		}
		String lowerKey = key.toLowerCase();
		if (lowerKey.startsWith(ACTIVE)) {
			return doMatch(ACTIVE_PATTERN, key, this.environment.getActiveProfiles());
		}
		else if (lowerKey.startsWith(DEFAULT)) {
			return doMatch(DEFAULT_PATTERN, key, this.environment.getDefaultProfiles());
		}

		return this.environment.getProperty(key);
	}

	private String doMatch(Pattern pattern, String key, String[] profiles) {
		if (profiles.length == 0) {
			return null;
		}
		if (profiles.length == 1) {
			return profiles[0];
		}
		Matcher matcher = pattern.matcher(key);
		if (matcher.matches()) {
			try {
				int index = Integer.parseInt(matcher.group(1));
				if (index < profiles.length) {
					return profiles[index];
				}
				LOGGER.warn("Index out of bounds for Spring default profiles: {}", index);
				return null;
			}
			catch (Exception ex) {
				LOGGER.warn("Unable to parse {} as integer value", matcher.group(1));
				return null;
			}

		}
		return String.join(",", profiles);
	}

	@Override
	public String lookup(LogEvent event, String key) {
		return lookup((key));
	}

	@Override
	public void setLoggerContext(final LoggerContext loggerContext) {
		if (loggerContext != null) {
			this.environment = (Environment) loggerContext.getObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
		}
		else {
			LOGGER.warn("Attempt to set LoggerContext reference to null in SpringLookup");
		}
	}

}
