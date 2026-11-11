/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.availability;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;

/**
 * Provides {@link AvailabilityState availability state} information for the application.
 * <p>
 * Components can inject this class to get the current state information. To update the
 * state of the application an {@link AvailabilityChangeEvent} should be
 * {@link ApplicationContext#publishEvent published} to the application context with
 * directly or through {@link AvailabilityChangeEvent#publish}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface ApplicationAvailability {

	/**
	 * Return the {@link LivenessState} of the application.
	 * @return the liveness state
	 */
	default LivenessState getLivenessState() {
		return getState(LivenessState.class, LivenessState.BROKEN);
	}

	/**
	 * Return the {@link ReadinessState} of the application.
	 * @return the readiness state
	 */
	default ReadinessState getReadinessState() {
		return getState(ReadinessState.class, ReadinessState.REFUSING_TRAFFIC);
	}

	/**
	 * Return {@link AvailabilityState} information for the application.
	 * @param <S> the state type
	 * @param stateType the state type
	 * @param defaultState the default state to return if no event of the given type has
	 * been published yet (must not be {@code null}).
	 * @return the readiness state
	 * @see #getState(Class)
	 */
	<S extends AvailabilityState> S getState(Class<S> stateType, S defaultState);

	/**
	 * Return {@link AvailabilityState} information for the application.
	 * @param <S> the state type
	 * @param stateType the state type
	 * @return the readiness state or {@code null} if no event of the given type has been
	 * published yet
	 * @see #getState(Class, AvailabilityState)
	 */
	<S extends AvailabilityState> @Nullable S getState(Class<S> stateType);

	/**
	 * Return the last {@link AvailabilityChangeEvent} received for a given state type.
	 * @param <S> the state type
	 * @param stateType the state type
	 * @return the readiness state or {@code null} if no event of the given type has been
	 * published yet
	 */
	<S extends AvailabilityState> @Nullable AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType);

}
