/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testsupport.junit;

import java.util.Arrays;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * Evaluates {@link DisabledOnOs}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class DisabledOnOsCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		if (context.getElement().isEmpty()) {
			return ConditionEvaluationResult.enabled("No element for @DisabledOnOs found");
		}
		MergedAnnotation<DisabledOnOs> annotation = MergedAnnotations
			.from(context.getElement().get(), SearchStrategy.TYPE_HIERARCHY)
			.get(DisabledOnOs.class);
		if (!annotation.isPresent()) {
			return ConditionEvaluationResult.enabled("No @DisabledOnOs found");
		}
		return evaluate(annotation.synthesize());
	}

	private ConditionEvaluationResult evaluate(DisabledOnOs annotation) {
		String architecture = System.getProperty("os.arch");
		String os = System.getProperty("os.name");
		boolean onDisabledOs = Arrays.stream(annotation.os()).anyMatch(OS::isCurrentOs);
		boolean onDisabledArchitecture = Arrays.asList(annotation.architecture()).contains(architecture);
		if (onDisabledOs && onDisabledArchitecture) {
			String reason = annotation.disabledReason().isEmpty()
					? String.format("Disabled on OS = %s, architecture = %s", os, architecture)
					: annotation.disabledReason();
			return ConditionEvaluationResult.disabled(reason);
		}
		return ConditionEvaluationResult
			.enabled(String.format("Enabled on OS = %s, architecture = %s", os, architecture));
	}

}
