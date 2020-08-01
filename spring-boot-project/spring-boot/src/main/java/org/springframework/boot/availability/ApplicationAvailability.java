/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.availability;

import org.springframework.context.ApplicationContext;

/**
 * Provides {@link AvailabilityState availability state} information for the application.
 * <p>
 * Components can inject this class to get the current state information. To update the
 * state of the application an {@link AvailabilityChangeEvent} should be
 * {@link ApplicationContext#publishEvent published} to the application context with
 * directly or via {@link AvailabilityChangeEvent#publish}.
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
	 * been published yet (must not be {@code null}.
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
	<S extends AvailabilityState> S getState(Class<S> stateType);

	/**
	 * Return the last {@link AvailabilityChangeEvent} received for a given state type.
	 * @param <S> the state type
	 * @param stateType the state type
	 * @return the readiness state or {@code null} if no event of the given type has been
	 * published yet
	 */
	<S extends AvailabilityState> AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType);

}
