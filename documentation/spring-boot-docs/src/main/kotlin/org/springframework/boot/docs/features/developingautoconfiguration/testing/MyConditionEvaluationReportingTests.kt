/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.testing

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class MyConditionEvaluationReportingTests {

	@Test
	fun autoConfigTest() {
		ApplicationContextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run { context: AssertableApplicationContext? ->
				// Test something...
			}
	}

}
