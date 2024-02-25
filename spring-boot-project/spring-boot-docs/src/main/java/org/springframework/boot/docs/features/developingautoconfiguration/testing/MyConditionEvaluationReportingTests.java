/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docs.features.developingautoconfiguration.testing;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * MyConditionEvaluationReportingTests class.
 */
class MyConditionEvaluationReportingTests {

	/**
	 * Test method to verify the auto configuration functionality.
	 *
	 * This method initializes the ApplicationContextRunner with a
	 * ConditionEvaluationReportLoggingListener for logging the condition evaluation
	 * report at the INFO log level. It then runs the test logic within the context,
	 * allowing for testing of the auto configuration functionality.
	 */
	@Test
	void autoConfigTest() {
		new ApplicationContextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> {
				// Test something...
			});
	}

}
