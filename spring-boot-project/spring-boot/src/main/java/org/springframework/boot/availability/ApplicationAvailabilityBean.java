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

import java.util.HashMap;
import java.util.Map;

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
		Class<? extends AvailabilityState> stateType = getStateType(event.getState());
		this.events.put(stateType, event);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends AvailabilityState> getStateType(AvailabilityState state) {
		if (state instanceof Enum) {
			return (Class<? extends AvailabilityState>) ((Enum<?>) state).getDeclaringClass();
		}
		return state.getClass();
	}

}
