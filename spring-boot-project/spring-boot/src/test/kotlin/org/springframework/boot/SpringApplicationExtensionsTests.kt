/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import org.springframework.beans.factory.getBean
import org.springframework.boot.web.servlet.server.MockServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.StandardEnvironment

/**
 * Tests for `SpringApplicationExtensions`.
 *
 * @author Sebastien Deleuze
 */
class SpringApplicationExtensionsTests {

	@Test
	fun `Kotlin runApplication() top level function`() {
		val context = runApplication<ExampleWebConfig>()
		assertNotNull(context)
	}

	@Test
	fun `Kotlin runApplication() top level function with a custom environment`() {
		val environment = StandardEnvironment()
		val context = runApplication<ExampleWebConfig> {
			setEnvironment(environment)
		}
		assertNotNull(context)
		assertEquals(environment, context.environment)
	}

	@Test
	fun `Kotlin runApplication(arg1, arg2) top level function`() {
		val context = runApplication<ExampleWebConfig>("--debug", "spring", "boot")
		val args = context.getBean<ApplicationArguments>()
		assertArrayEquals(arrayOf("spring", "boot"), args.nonOptionArgs.toTypedArray())
		assertTrue(args.containsOption("debug"))
	}

	@Test
	fun `Kotlin runApplication(arg1, arg2) top level function with a custom environment`() {
		val environment = StandardEnvironment()
		val context = runApplication<ExampleWebConfig>("--debug", "spring", "boot") {
			setEnvironment(environment)
		}
		val args = context.getBean<ApplicationArguments>()
		assertArrayEquals(arrayOf("spring", "boot"), args.nonOptionArgs.toTypedArray())
		assertTrue(args.containsOption("debug"))
		assertEquals(environment, context.environment)
	}

	@Configuration
	internal open class ExampleWebConfig {

		@Bean
		open fun webServer(): MockServletWebServerFactory {
			return MockServletWebServerFactory()
		}

	}

}
