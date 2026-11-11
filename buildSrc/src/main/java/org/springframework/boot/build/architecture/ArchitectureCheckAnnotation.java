/*
 * Copyright 2012-present the original author or authors.
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
	 * Configuration properties source.
	 */
	CONFIGURATION_PROPERTIES_SOURCE,

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
			CONFIGURATION_PROPERTIES_SOURCE.name(),
			"org.springframework.boot.context.properties.ConfigurationPropertiesSource",
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
