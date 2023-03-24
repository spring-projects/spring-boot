/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.logging;

import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Factory class used by {@link LoggingSystem#get(ClassLoader)} to find an actual
 * implementation.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public interface LoggingSystemFactory {

	/**
	 * Return a logging system implementation or {@code null} if no logging system is
	 * available.
	 * @param classLoader the class loader to use
	 * @return a logging system
	 */
	LoggingSystem getLoggingSystem(ClassLoader classLoader);

	/**
	 * Return a {@link LoggingSystemFactory} backed by {@code spring.factories}.
	 * @return a {@link LoggingSystemFactory} instance
	 */
	static LoggingSystemFactory fromSpringFactories() {
		return new DelegatingLoggingSystemFactory(
				(classLoader) -> SpringFactoriesLoader.loadFactories(LoggingSystemFactory.class, classLoader));
	}

}
