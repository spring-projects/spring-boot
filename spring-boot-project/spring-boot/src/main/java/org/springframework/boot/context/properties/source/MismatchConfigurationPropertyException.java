/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.properties.source;

/**
 * Exception thrown when a configuration property mismatch occurs.
 * This exception is raised when there is a mismatch between different configuration properties,
 * resulting in a configuration inconsistency.
 *
 * @author Ilya Starchenko
 * @since 3.1.0
 */
public class MismatchConfigurationPropertyException extends RuntimeException {

	public MismatchConfigurationPropertyException(String expectedPropertyName, String mismatchPropertyName) {
		super("Configuration property name '" + mismatchPropertyName + "' is used, but expected '" + expectedPropertyName + "'");
	}

	/**
	 * Throw an exception if provided mismatch property is not null.
	 * @param property the mismatch property
	 * @param expectedPropertyName the expected property name
	 * @param mismatchPropertyName the mismatch property name
	 */
	public static void throwIfMismatch(String property, String expectedPropertyName, String mismatchPropertyName) {
		if (property != null) {
			throw new MismatchConfigurationPropertyException(expectedPropertyName, mismatchPropertyName);
		}
	}
}
