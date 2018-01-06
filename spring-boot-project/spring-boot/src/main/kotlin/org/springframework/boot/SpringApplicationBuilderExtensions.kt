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

package org.springframework.boot

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer

/**
 * Top level function acting as a Kotlin shortcut allowing to write
 * `buildApplication<FooApplication>()` instead of
 * `SpringApplicationBuilder(FooApplication::class.java)`.
 *
 * @author Juan Medina
 * @since 2.0.0
 * @see SpringApplicationBuilder
 */
inline fun <reified T : Any> buildApplication(): SpringApplicationBuilder =
        SpringApplicationBuilder(T::class.java)

/**
 * Top level function acting as a Kotlin shortcut allowing to write
 * `buildApplication<FooApplication>() { // ApplicationContextInitializer ... }`
 * instead of creating the builder and invoking `initializers()`.
 *
 * @author Juan Medina
 * @since 2.0.0
 * @see SpringApplicationBuilder.initializers
 */
inline fun <reified T : Any> buildApplication(initializers: () -> ApplicationContextInitializer<*>): SpringApplicationBuilder =
        SpringApplicationBuilder(T::class.java).initializers(initializers())

/**
 * High level extensions acting as a Kotlin shortcut allowing to write
 * `SpringApplicationBuilder.sources<FooApplication>()`
 * instead of `SpringApplicationBuilder.sources(FooApplication::class.java)`.
 *
 * @author Juan Medina
 * @since 2.0.0
 * @see SpringApplicationBuilder.sources
 */
inline fun <reified T : Any> SpringApplicationBuilder.sources() = this.sources(T::class.java)
