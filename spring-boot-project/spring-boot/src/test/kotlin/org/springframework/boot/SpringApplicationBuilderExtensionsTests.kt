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

import org.junit.Assert.assertNotNull
import org.junit.Test

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.server.MockServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.beans

/**
 * Tests for `SpringApplicationBuilderExtensionsTests`.
 *
 * @author Juan Medina
 */
class SpringApplicationBuilderExtensionsTests {

    @Test
    fun `Kotlin buildApplication() top level function`() {
        val context = buildApplication<MockWebServerConfiguration>().run()

        assertNotNull(context)
        assertNotNull(context.getBean(MockServletWebServerFactory::class.java))
    }

    @Test
    fun `Kotlin buildApplication top level function with initializers`() {
        val context = buildApplication<SimpleConfiguration> {
            beans {
                bean {
                    MockServletWebServerFactory()
                }
            }
        }.run()

        assertNotNull(context)
        assertNotNull(context.getBean(MockServletWebServerFactory::class.java))
    }

    @Test
    fun `Kotlin high level function sources()`() {
        val context = SpringApplicationBuilder().sources<MockWebServerConfiguration>().run()

        assertNotNull(context)
        assertNotNull(context.getBean(MockServletWebServerFactory::class.java))
    }

    @Configuration
    internal open class SimpleConfiguration

    @Configuration
    internal open class MockWebServerConfiguration {
        @Bean
        open fun webServer(): MockServletWebServerFactory {
            return MockServletWebServerFactory()
        }
    }
}
