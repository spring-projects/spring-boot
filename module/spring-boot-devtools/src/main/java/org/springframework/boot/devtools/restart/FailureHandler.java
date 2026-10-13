/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

/**
 * Strategy used to handle launch failures.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@FunctionalInterface
public interface FailureHandler {

	/**
	 * {@link FailureHandler} that always aborts.
	 */
	FailureHandler NONE = (failure) -> Outcome.ABORT;

	/**
	 * Handle a run failure. Implementations may block, for example to wait until specific
	 * files are updated.
	 * @param failure the exception
	 * @return the outcome
	 */
	Outcome handle(Throwable failure);

	/**
	 * Various outcomes for the handler.
	 */
	enum Outcome {

		/**
		 * Abort the relaunch.
		 */
		ABORT,

		/**
		 * Try again to relaunch the application.
		 */
		RETRY

	}

}
