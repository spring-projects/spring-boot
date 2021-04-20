/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

/**
 * Bean that provides an {@link ApplicationAvailability} implementation by listening for
 * {@link AvailabilityChangeEvent change events}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.3.0
 * @see ApplicationAvailability
 */
public class ApplicationAvailabilityBean
		implements ApplicationAvailability, ApplicationListener<AvailabilityChangeEvent<?>> {

	private static final Log logger = LogFactory.getLog(ApplicationAvailability.class);

	private final Map<Class<? extends AvailabilityState>, AvailabilityChangeEvent<?>> events = new HashMap<>();

	@Override
	public <S extends AvailabilityState> S getState(Class<S> stateType, S defaultState) {
		Assert.notNull(stateType, "StateType must not be null");
		Assert.notNull(defaultState, "DefaultState must not be null");
		S state = getState(stateType);
		return (state != null) ? state : defaultState;
	}

	@Override
	public <S extends AvailabilityState> S getState(Class<S> stateType) {
		AvailabilityChangeEvent<S> event = getLastChangeEvent(stateType);
		return (event != null) ? event.getState() : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends AvailabilityState> AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType) {
		return (AvailabilityChangeEvent<S>) this.events.get(stateType);
	}

	@Override
	public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
		logStateChange(event);
		Class<? extends AvailabilityState> stateType = getStateType(event.getState());
		this.events.put(stateType, event);
	}

	private void logStateChange(AvailabilityChangeEvent<?> event) {
		Class<? extends AvailabilityState> stateType = getStateType(event.getState());
		StringBuilder message = new StringBuilder(
				"Application availability state " + stateType.getSimpleName() + " changed");
		AvailabilityChangeEvent<? extends AvailabilityState> lastChangeEvent = getLastChangeEvent(stateType);
		if (lastChangeEvent != null) {
			message.append(" from " + lastChangeEvent.getState());
		}
		message.append(" to " + event.getState());
		if (event.getSource() != null) {
			if (event.getSource() instanceof Throwable) {
				message.append(": " + event.getSource());
			}
			else if (!(event.getSource() instanceof ApplicationEventPublisher)) {
				message.append(": " + event.getSource().getClass().getName());
			}
		}
		logger.info(message);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends AvailabilityState> getStateType(AvailabilityState state) {
		if (state instanceof Enum) {
			return (Class<? extends AvailabilityState>) ((Enum<?>) state).getDeclaringClass();
		}
		return state.getClass();
	}

}
