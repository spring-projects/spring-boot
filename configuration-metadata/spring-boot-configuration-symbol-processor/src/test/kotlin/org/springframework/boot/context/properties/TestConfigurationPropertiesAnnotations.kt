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

package org.springframework.boot.context.properties

/**
 * Test copy of Spring Boot's `@ConfigurationProperties`, declared with the same fully
 * qualified name so that the processor can be tested without depending on Spring Boot.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ConfigurationProperties(val value: String = "", val prefix: String = "")

/**
 * Test copy of Spring Boot's `@ConfigurationPropertiesSource`.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.CLASS)
annotation class ConfigurationPropertiesSource

/**
 * Test copy of Spring Boot's `@NestedConfigurationProperty`.
 *
 * @author Areg Iazychian
 */
@Target(
	AnnotationTarget.FIELD,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.VALUE_PARAMETER,
)
annotation class NestedConfigurationProperty

/**
 * Test copy of Spring Boot's `@DeprecatedConfigurationProperty`.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class DeprecatedConfigurationProperty(
	val reason: String = "",
	val replacement: String = "",
	val since: String = "",
)
