/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics;

import org.jspecify.annotations.Nullable;

/**
 * {@link RuntimeException} that includes a {@link FailureAnalysis}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public class FailureAnalyzedException extends RuntimeException {

	private final @Nullable String action;

	/**
	 * Create a new {@link FailureAnalyzedException} instance.
	 * @param description the {@link FailureAnalysis} description
	 * @param action the {@link FailureAnalysis} action
	 */
	public FailureAnalyzedException(String description, @Nullable String action) {
		super(description);
		this.action = action;
	}

	/**
	 * Create a new {@link FailureAnalyzedException} instance.
	 * @param description the {@link FailureAnalysis} description
	 * @param action the {@link FailureAnalysis} action
	 * @param cause the cause of the exception
	 */
	public FailureAnalyzedException(String description, @Nullable String action, Throwable cause) {
		super(description, cause);
		this.action = action;
	}

	/**
	 * Return the {@link FailureAnalysis} to use for this exception.
	 * @return the failure analysis
	 */
	public FailureAnalysis analysis() {
		return new FailureAnalysis(getMessage(), this.action, this);
	}

	static @Nullable FailureAnalysis analyze(Throwable failure) {
		while (failure != null) {
			if (failure instanceof FailureAnalyzedException failureAnalyzedException) {
				return failureAnalyzedException.analysis();
			}
			failure = failure.getCause();
		}
		return null;
	}

}
