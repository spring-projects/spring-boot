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

package org.springframework.boot.configurationprocessor.ksp

/**
 * Fully qualified names of the annotations that drive configuration metadata generation.
 *
 * The names are referenced as strings so that the processor can run without Spring Boot on
 * its classpath, mirroring the behavior of the Java annotation processor.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
internal object ConfigurationPropertiesAnnotations {

	const val CONFIGURATION_PROPERTIES = "org.springframework.boot.context.properties.ConfigurationProperties"

	const val CONFIGURATION_PROPERTIES_SOURCE =
		"org.springframework.boot.context.properties.ConfigurationPropertiesSource"

	const val NESTED_CONFIGURATION_PROPERTY =
		"org.springframework.boot.context.properties.NestedConfigurationProperty"

	const val DEPRECATED_CONFIGURATION_PROPERTY =
		"org.springframework.boot.context.properties.DeprecatedConfigurationProperty"

	const val CONSTRUCTOR_BINDING = "org.springframework.boot.context.properties.bind.ConstructorBinding"

	const val DEFAULT_VALUE = "org.springframework.boot.context.properties.bind.DefaultValue"

	const val NAME = "org.springframework.boot.context.properties.bind.Name"

	const val AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired"

	const val READ_OPERATION = "org.springframework.boot.actuate.endpoint.annotation.ReadOperation"

	const val ENDPOINT_ACCESS_ENUM = "org.springframework.boot.actuate.endpoint.Access"

	/**
	 * The annotations that declare an actuator endpoint, in the order in which they are
	 * considered.
	 */
	val ENDPOINT_ANNOTATIONS = listOf(
		"org.springframework.boot.actuate.endpoint.annotation.Endpoint",
		"org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint",
		"org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint",
		"org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint",
		"org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint",
		"org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint",
	)

	const val JAVA_DEPRECATED = "java.lang.Deprecated"

	const val KOTLIN_DEPRECATED = "kotlin.Deprecated"

}
