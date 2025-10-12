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

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation for {@link Log4J2LoggingSystem}.
 *
 * @author Piotr P. Karwasz
 * @author Stephane Nicoll
 */
class Log4J2RuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		if (ClassUtils.isPresent(Log4J2LoggingSystem.Factory.LOG4J_CORE_CONTEXT_FACTORY, classLoader)) {
			registerLog4j2Hints(hints, classLoader);
		}
	}

	private void registerLog4j2Hints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerTypeIfPresent(classLoader, Log4J2LoggingSystem.Factory.LOG4J_CORE_CONTEXT_FACTORY);
		// Register default Log4j2 configuration files
		hints.resources().registerPattern("org/springframework/boot/logging/log4j2/log4j2.xml");
		hints.resources().registerPattern("org/springframework/boot/logging/log4j2/log4j2-file.xml");
		hints.resources().registerPattern("log4j2.springboot");
		// Declares the types that Log4j2LoggingSystem checks for existence reflectively.
		hints.reflection().registerTypeIfPresent(classLoader, Log4J2LoggingSystem.LOG4J_BRIDGE_HANDLER);
		hints.reflection().registerTypeIfPresent(classLoader, Log4J2LoggingSystem.LOG4J_LOG_MANAGER);
	}

}
