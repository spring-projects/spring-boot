/*
 * Copyright 2012-present the original author or authors.
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
