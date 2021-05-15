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

package org.springframework.boot.buildpack.platform.system;

/**
 * Provides access to environment variable values.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface Environment {

	/**
	 * Standard {@link Environment} implementation backed by
	 * {@link System#getenv(String)}.
	 */
	Environment SYSTEM = System::getenv;

	/**
	 * Gets the value of the specified environment variable.
	 * @param name the name of the environment variable
	 * @return the string value of the variable, or {@code null} if the variable is not
	 * defined in the environment
	 */
	String get(String name);

}
