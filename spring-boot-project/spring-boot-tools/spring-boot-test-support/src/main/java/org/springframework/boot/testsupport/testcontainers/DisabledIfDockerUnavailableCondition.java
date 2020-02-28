/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.testsupport.testcontainers;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * An {@link ExecutionCondition} that disables execution if Docker is unavailable.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DisabledIfDockerUnavailableCondition implements ExecutionCondition {

	private static final String SILENCE_PROPERTY = "visibleassertions.silence";

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("Docker available");

	private static final ConditionEvaluationResult DISABLED = ConditionEvaluationResult.disabled("Docker unavailable");

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		String originalSilenceValue = System.getProperty(SILENCE_PROPERTY);
		try {
			DockerClientFactory.instance().client();
			return ENABLED;
		}
		catch (Throwable ex) {
			return DISABLED;
		}
		finally {
			if (originalSilenceValue != null) {
				System.setProperty(SILENCE_PROPERTY, originalSilenceValue);
			}
			else {
				System.clearProperty(SILENCE_PROPERTY);
			}
		}
	}

}
