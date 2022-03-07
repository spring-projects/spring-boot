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

package org.springframework.boot.testsupport.junit;

import java.util.List;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * Evaluates {@link DisabledOnOs}.
 *
 * @author Moritz Halbritter
 */
class DisabledOnOsCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		List<DisabledOnOs> annotations = AnnotationUtils.findRepeatableAnnotations(context.getElement(),
				DisabledOnOs.class);
		if (annotations.isEmpty()) {
			return ConditionEvaluationResult.enabled("No @DisabledOnOs found");
		}
		return evaluate(annotations);
	}

	private ConditionEvaluationResult evaluate(List<DisabledOnOs> annotations) {
		String architecture = System.getProperty("os.arch");
		String os = System.getProperty("os.name");
		for (DisabledOnOs annotation : annotations) {
			if (annotation.os().isCurrentOs() && annotation.architecture().equals(architecture)) {
				String reason = annotation.disabledReason().isEmpty()
						? String.format("Disabled on OS = %s, architecture = %s", os, architecture)
						: annotation.disabledReason();
				return ConditionEvaluationResult.disabled(reason);
			}
		}
		return ConditionEvaluationResult
				.enabled(String.format("Enabled on OS = %s, architecture = %s", os, architecture));
	}

}
