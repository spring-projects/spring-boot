/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.context

import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestContextManager

/**
 * Kotlin tests for [SpringBootContextLoader].
 */
class SpringBootContextLoaderKotlinTests {

	@Test
	fun `when UseMainMethod ALWAYS and main method throws exception`() {
		val testContext = ExposedTestContextManager(
			UseMainMethodAlwaysAndKotlinMainMethodThrowsException::class.java
		).exposedTestContext
		assertThatIllegalStateException().isThrownBy { testContext.applicationContext }
			.havingCause()
			.withMessageContaining("ThrownFromMain")
	}

	/**
	 * [TestContextManager] which exposes the [TestContext].
	 */
	internal class ExposedTestContextManager(testClass: Class<*>) : TestContextManager(testClass) {
		val exposedTestContext: TestContext
			get() = super.getTestContext()
	}

	@SpringBootTest(classes = [KotlinApplicationWithMainThrowingException::class], useMainMethod = UseMainMethod.ALWAYS)
	internal class UseMainMethodAlwaysAndKotlinMainMethodThrowsException

}