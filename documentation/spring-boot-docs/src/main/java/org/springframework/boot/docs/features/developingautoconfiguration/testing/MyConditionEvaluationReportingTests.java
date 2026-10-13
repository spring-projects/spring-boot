/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.testing;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MyConditionEvaluationReportingTests {

	@Test
	void autoConfigTest() {
		new ApplicationContextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> {
				// Test something...
			});
	}

}
