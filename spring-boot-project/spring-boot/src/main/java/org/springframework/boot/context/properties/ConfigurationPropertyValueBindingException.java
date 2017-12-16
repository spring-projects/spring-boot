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

import org.springframework.util.ClassUtils;

/**
 * Exception thrown when a {@code @ConfigurationPropertyValue} annotation on a field or
 * method conflicts with another annotation.
 *
 * @author Tom Hombergs
 * @since 2.0.0
 */
public final class ConfigurationPropertyValueBindingException extends RuntimeException {

	private ConfigurationPropertyValueBindingException(String message) {
		super(message);
	}

	public static ConfigurationPropertyValueBindingException invalidUseOnMethod(
			Class<?> targetClass, String methodName, String reason) {
		return new ConfigurationPropertyValueBindingException(String.format(
				"Invalid use of annotation %s on method '%s' of class '%s': %s",
				ClassUtils.getShortName(ConfigurationPropertyValue.class), methodName,
				targetClass.getName(), reason));
	}

	public static ConfigurationPropertyValueBindingException invalidUseOnField(
			Class<?> targetClass, String fieldName, String reason) {
		return new ConfigurationPropertyValueBindingException(String.format(
				"Invalid use of annotation %s on field '%s' of class '%s': %s",
				ClassUtils.getShortName(ConfigurationPropertyValue.class), fieldName,
				targetClass.getName(), reason));
	}

}
