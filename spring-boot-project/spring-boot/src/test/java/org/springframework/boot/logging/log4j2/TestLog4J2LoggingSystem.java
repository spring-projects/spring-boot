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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configuration;

class TestLog4J2LoggingSystem extends Log4J2LoggingSystem {

	private final List<String> availableClasses = new ArrayList<>();

	TestLog4J2LoggingSystem() {
		super(TestLog4J2LoggingSystem.class.getClassLoader());
	}

	Configuration getConfiguration() {
		return ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).getConfiguration();
	}

	@Override
	protected boolean isClassAvailable(String className) {
		return this.availableClasses.contains(className);
	}

	void availableClasses(String... classNames) {
		Collections.addAll(this.availableClasses, classNames);
	}

}
