/*
 * Copyright 2012-present the original author or authors.
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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.jspecify.annotations.Nullable;

class TestLog4J2LoggingSystem extends Log4J2LoggingSystem {

	private boolean disableSelfInitialization;

	TestLog4J2LoggingSystem(String contextName) {
		// Tests add resources to the thread context classloader
		super(Thread.currentThread().getContextClassLoader(), new LoggerContext(contextName));
		getLoggerContext().start();
	}

	Configuration getConfiguration() {
		return getLoggerContext().getConfiguration();
	}

	@Override
	protected @Nullable String getSelfInitializationConfig() {
		return this.disableSelfInitialization ? null : super.getSelfInitializationConfig();
	}

	void disableSelfInitialization() {
		this.disableSelfInitialization = true;
	}

}
