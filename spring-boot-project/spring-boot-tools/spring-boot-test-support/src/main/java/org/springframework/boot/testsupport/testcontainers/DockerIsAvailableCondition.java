/*
 * Copyright 2012-2019 the original author or authors.
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
 * {@link ExecutionCondition} for
 * {@link DisabledWithoutDockerTestcontainers @DisabledWithoutDockerTestcontainers}.
 *
 * @author Andy Wilkinson
 */
final class DockerIsAvailableCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		try {
			DockerClientFactory.instance().client();
			return ConditionEvaluationResult.enabled("Docker is available");
		}
		catch (Throwable ex) {
			return ConditionEvaluationResult.disabled("Docker is not available: " + ex.getMessage());
		}
	}

}
