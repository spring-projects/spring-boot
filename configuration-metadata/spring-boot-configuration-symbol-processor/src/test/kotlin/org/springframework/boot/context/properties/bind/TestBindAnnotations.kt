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

package org.springframework.boot.context.properties.bind

/**
 * Test copy of Spring Boot's `@ConstructorBinding`, declared with the same fully qualified
 * name so that the processor can be tested without depending on Spring Boot.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ConstructorBinding

/**
 * Test copy of Spring Boot's `@DefaultValue`.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultValue(vararg val value: String)

/**
 * Test copy of Spring Boot's `@Name`.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Name(val value: String)
