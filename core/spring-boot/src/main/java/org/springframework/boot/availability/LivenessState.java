/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.availability;

/**
 * "Liveness" state of the application.
 * <p>
 * An application is considered live when it's running with a correct internal state.
 * "Liveness" failure means that the internal state of the application is broken and we
 * cannot recover from it. As a result, the platform should restart the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public enum LivenessState implements AvailabilityState {

	/**
	 * The application is running and its internal state is correct.
	 */
	CORRECT,

	/**
	 * The application is running but its internal state is broken.
	 */
	BROKEN

}
