/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.availability;

/**
 * "Readiness" state of the application.
 * <p>
 * An application is considered ready when it's {@link LivenessState live} and willing to
 * accept traffic. "Readiness" failure means that the application is not able to accept
 * traffic and that the infrastructure should stop routing requests to it.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public enum ReadinessState implements AvailabilityState {

	/**
	 * The application is ready to receive traffic.
	 */
	ACCEPTING_TRAFFIC,

	/**
	 * The application is not willing to receive traffic.
	 */
	REFUSING_TRAFFIC

}
