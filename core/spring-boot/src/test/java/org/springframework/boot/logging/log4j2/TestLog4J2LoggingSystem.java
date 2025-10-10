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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.jspecify.annotations.Nullable;

class TestLog4J2LoggingSystem extends Log4J2LoggingSystem {

	private final List<String> availableClasses = new ArrayList<>();

	private String @Nullable [] standardConfigLocations;

	TestLog4J2LoggingSystem() {
		super(TestLog4J2LoggingSystem.class.getClassLoader());
	}

	Configuration getConfiguration() {
		return getLoggerContext().getConfiguration();
	}

	private LoggerContext getLoggerContext() {
		return (LoggerContext) LogManager.getContext(false);
	}

	@Override
	protected boolean isClassAvailable(ClassLoader classLoader, String className) {
		return this.availableClasses.contains(className);
	}

	void availableClasses(String... classNames) {
		Collections.addAll(this.availableClasses, classNames);
	}

	@Override
	protected String[] getStandardConfigLocations() {
		return (this.standardConfigLocations != null) ? this.standardConfigLocations
				: super.getStandardConfigLocations();
	}

	void setStandardConfigLocations(boolean standardConfigLocations) {
		this.standardConfigLocations = (!standardConfigLocations) ? new String[0] : null;
	}

	void setStandardConfigLocations(String[] standardConfigLocations) {
		this.standardConfigLocations = standardConfigLocations;
	}

}
