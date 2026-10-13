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

package org.springframework.boot.actuate.endpoint.annotation

import org.springframework.boot.actuate.endpoint.Access

/**
 * Test copy of Spring Boot's actuator `@Endpoint`, declared with the same fully qualified
 * name so that the processor can be tested without depending on Spring Boot.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.CLASS)
annotation class Endpoint(val id: String = "", val defaultAccess: Access = Access.UNRESTRICTED)

/**
 * Test copy of Spring Boot's actuator `@ReadOperation`.
 *
 * @author Areg Iazychian
 */
@Target(AnnotationTarget.FUNCTION)
annotation class ReadOperation
