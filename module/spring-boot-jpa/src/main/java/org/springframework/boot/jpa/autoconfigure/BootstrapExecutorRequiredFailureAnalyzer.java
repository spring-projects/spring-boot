/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jpa.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * {@link FailureAnalyzer} for {@link BootstrapExecutorRequiredException}.
 *
 * @author Phillip Webb
 */
class BootstrapExecutorRequiredFailureAnalyzer extends AbstractFailureAnalyzer<BootstrapExecutorRequiredException> {

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, BootstrapExecutorRequiredException cause) {
		StringBuilder action = new StringBuilder();
		action.append(actionPreamble(cause.getPropertyName()));
		action.append("\t- With an auto-configured task executor "
				+ "(you may need to set 'spring.task.execution.mode' to 'force').\n");
		action.append("\t- With an AsyncTaskExecutor bean named 'applicationTaskExecutor'.\n");
		action.append("\t- Using a EntityManagerFactoryBuilderCustomizer.\n");
		return new FailureAnalysis(cause.getMessage(), action.toString(), cause);
	}

	private String actionPreamble(@Nullable String propertyName) {
		return (propertyName != null)
				? "Use a different '%s' value or provide a bootstrap executor using one of the following methods:\n"
					.formatted(propertyName)
				: "Provide a bootstrap executor using one of the following methods:\n";
	}

}
