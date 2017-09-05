/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.ClassUtils;

/**
 * Exception thrown when a {@code @ConfigurationProperties} annotated object failed to be
 * bound.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ConfigurationPropertiesBindingException extends RuntimeException {

	private final Class<?> targetClass;

	public ConfigurationPropertiesBindingException(Class<?> targetClass, String message,
			Throwable cause) {
		super("Could not bind properties to '" + ClassUtils.getShortName(targetClass)
				+ "': " + message, cause);
		this.targetClass = targetClass;
	}

	/**
	 * Return the target type of the object that failed to be bound.
	 * @return the target {@link Class}
	 */
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * Retrieve the innermost cause of this exception, if any.
	 * @return the innermost exception, or {@code null} if none
	 */
	public Throwable getRootCause() {
		return NestedExceptionUtils.getRootCause(this);
	}

}
