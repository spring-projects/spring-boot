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

package org.springframework.boot.logging;

import org.springframework.boot.ApplicationPid;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;

/**
 * Utility to set system properties that can later be used by log configuration files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class LoggingSystemProperties {

	static final String PID_KEY = "PID";

	static final String EXCEPTION_CONVERSION_WORD = "LOG_EXCEPTION_CONVERSION_WORD";

	static final String CONSOLE_LOG_PATTERN = "CONSOLE_LOG_PATTERN";

	static final String FILE_LOG_PATTERN = "FILE_LOG_PATTERN";

	static final String LOG_LEVEL_PATTERN = "LOG_LEVEL_PATTERN";

	private final Environment environment;

	LoggingSystemProperties(Environment environment) {
		this.environment = environment;
	}

	public void apply() {
		apply(null);
	}

	public void apply(LogFile logFile) {
		RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(
				this.environment, "logging.");
		setSystemProperty(propertyResolver, EXCEPTION_CONVERSION_WORD,
				"exception-conversion-word");
		setSystemProperty(propertyResolver, CONSOLE_LOG_PATTERN, "pattern.console");
		setSystemProperty(propertyResolver, FILE_LOG_PATTERN, "pattern.file");
		setSystemProperty(propertyResolver, LOG_LEVEL_PATTERN, "pattern.level");
		setSystemProperty(PID_KEY, new ApplicationPid().toString());
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
	}

	private void setSystemProperty(RelaxedPropertyResolver propertyResolver,
			String systemPropertyName, String propertyName) {
		setSystemProperty(systemPropertyName, propertyResolver.getProperty(propertyName));
	}

	private void setSystemProperty(String name, String value) {
		if (System.getProperty(name) == null && value != null) {
			System.setProperty(name, value);
		}
	}

}
