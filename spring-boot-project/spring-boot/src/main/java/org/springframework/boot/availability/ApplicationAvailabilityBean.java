/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	private final Map<Class<? extends AvailabilityState>, AvailabilityChangeEvent<?>> events = new ConcurrentHashMap<>();

	private final Log logger;

	/**
     * Constructs a new ApplicationAvailabilityBean.
     * 
     * @param log the log instance to be used for logging
     */
    public ApplicationAvailabilityBean() {
		this(LogFactory.getLog(ApplicationAvailabilityBean.class));
	}

	/**
     * Constructs a new ApplicationAvailabilityBean with the specified logger.
     * 
     * @param logger the logger to be used for logging
     */
    ApplicationAvailabilityBean(Log logger) {
		this.logger = logger;
	}

	/**
     * Returns the state of the specified availability state type.
     * 
     * @param stateType the class representing the availability state type
     * @param defaultState the default state to return if the specified state type is not found
     * @return the state of the specified availability state type, or the default state if not found
     * @throws IllegalArgumentException if the stateType or defaultState is null
     */
    @Override
	public <S extends AvailabilityState> S getState(Class<S> stateType, S defaultState) {
		Assert.notNull(stateType, "StateType must not be null");
		Assert.notNull(defaultState, "DefaultState must not be null");
		S state = getState(stateType);
		return (state != null) ? state : defaultState;
	}

	/**
     * Returns the last availability state of the specified type.
     * 
     * @param stateType the class representing the availability state type
     * @return the last availability state of the specified type, or null if no state of the specified type is available
     * @param <S> the type of the availability state
     */
    @Override
	public <S extends AvailabilityState> S getState(Class<S> stateType) {
		AvailabilityChangeEvent<S> event = getLastChangeEvent(stateType);
		return (event != null) ? event.getState() : null;
	}

	/**
     * Returns the last availability change event of the specified state type.
     * 
     * @param stateType the class representing the availability state type
     * @return the last availability change event of the specified state type, or null if no event of the specified state type has occurred
     * @throws ClassCastException if the last event is not of the specified state type
     * 
     * @since 1.0.0
     */
    @Override
	@SuppressWarnings("unchecked")
	public <S extends AvailabilityState> AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType) {
		return (AvailabilityChangeEvent<S>) this.events.get(stateType);
	}

	/**
     * This method is called when an AvailabilityChangeEvent is triggered.
     * It stores the event in a map based on the state type.
     * 
     * @param event The AvailabilityChangeEvent that was triggered
     */
    @Override
	public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
		Class<? extends AvailabilityState> type = getStateType(event.getState());
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(getLogMessage(type, event));
		}
		this.events.put(type, event);
	}

	/**
     * Returns the log message for the given availability state change event.
     * 
     * @param <S> the type of the availability state
     * @param type the class representing the availability state
     * @param event the availability state change event
     * @return the log message for the availability state change event
     */
    private <S extends AvailabilityState> Object getLogMessage(Class<S> type, AvailabilityChangeEvent<?> event) {
		AvailabilityChangeEvent<S> lastChangeEvent = getLastChangeEvent(type);
		StringBuilder message = new StringBuilder(
				"Application availability state " + type.getSimpleName() + " changed");
		message.append((lastChangeEvent != null) ? " from " + lastChangeEvent.getState() : "");
		message.append(" to " + event.getState());
		message.append(getSourceDescription(event.getSource()));
		return message;
	}

	/**
     * Returns the description of the source.
     * 
     * @param source the source object
     * @return the description of the source
     */
    private String getSourceDescription(Object source) {
		if (source == null || source instanceof ApplicationEventPublisher) {
			return "";
		}
		return ": " + ((source instanceof Throwable) ? source : source.getClass().getName());
	}

	/**
     * Returns the type of the given AvailabilityState object.
     * 
     * @param state the AvailabilityState object whose type is to be determined
     * @return the type of the given AvailabilityState object
     */
    @SuppressWarnings("unchecked")
	private Class<? extends AvailabilityState> getStateType(AvailabilityState state) {
		Class<?> type = (state instanceof Enum) ? ((Enum<?>) state).getDeclaringClass() : state.getClass();
		return (Class<? extends AvailabilityState>) type;
	}

}
