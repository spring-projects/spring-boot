/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot

import org.springframework.context.ConfigurableApplicationContext

import kotlin.reflect.KClass

/**
 * Top level function acting as a Kotlin shortcut allowing to write
 * `runApplication<FooApplication>(arg1, arg2)` instead of
 * `SpringApplication.run(FooApplication::class.java, arg1, arg2)`.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
inline fun <reified T : Any> runApplication(vararg args: String): ConfigurableApplicationContext =
		SpringApplication.run(T::class.java, *args)

/**
 * Top level function acting as a Kotlin shortcut allowing to write
 * `runApplication<FooApplication>(arg1, arg2) { // SpringApplication customization ... }`
 * instead of instantiating `SpringApplication` class, customize it and then invoking
 * `run(arg1, arg2)`.
 *
 * @author Sebastien Deleuze
 * @since 2.0.0
 */
inline fun <reified T : Any> runApplication(vararg args: String, init: SpringApplication.() -> Unit): ConfigurableApplicationContext =
		SpringApplication(T::class.java).apply(init).run(*args)
