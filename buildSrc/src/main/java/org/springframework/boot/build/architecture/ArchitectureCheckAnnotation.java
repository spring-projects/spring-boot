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

package org.springframework.boot.build.architecture;

import java.util.Map;

/**
 * Annotations used in architecture checks, which can be overridden in architecture rule
 * tests.
 *
 * @author Scott Frederick
 * @author Stephane Nicoll
 */
public enum ArchitectureCheckAnnotation {

	/**
	 * Condition on class check.
	 */
	CONDITIONAL_ON_CLASS,

	/**
	 * Condition on missing bean check.
	 */
	CONDITIONAL_ON_MISSING_BEAN,

	/**
	 * Configuration properties bean.
	 */
	CONFIGURATION_PROPERTIES,

	/**
	 * Deprecated configuration property.
	 */
	DEPRECATED_CONFIGURATION_PROPERTY,

	/**
	 * Custom implementation to bind configuration properties.
	 */
	CONFIGURATION_PROPERTIES_BINDING;

	private static final Map<String, String> annotationNameToClassName = Map.of(CONDITIONAL_ON_CLASS.name(),
			"org.springframework.boot.autoconfigure.condition.ConditionalOnClass", CONDITIONAL_ON_MISSING_BEAN.name(),
			"org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean",
			CONFIGURATION_PROPERTIES.name(), "org.springframework.boot.context.properties.ConfigurationProperties",
			DEPRECATED_CONFIGURATION_PROPERTY.name(),
			"org.springframework.boot.context.properties.DeprecatedConfigurationProperty",
			CONFIGURATION_PROPERTIES_BINDING.name(),
			"org.springframework.boot.context.properties.ConfigurationPropertiesBinding");

	static Map<String, String> asMap() {
		return annotationNameToClassName;
	}

	static String classFor(Map<String, String> annotationProperty, ArchitectureCheckAnnotation annotation) {
		String name = annotation.name();
		return annotationProperty.getOrDefault(name, asMap().get(name));
	}

}
