/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context

import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
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

