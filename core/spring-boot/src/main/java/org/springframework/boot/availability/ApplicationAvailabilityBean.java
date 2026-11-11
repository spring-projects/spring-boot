/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.availability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

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

	public ApplicationAvailabilityBean() {
		this(LogFactory.getLog(ApplicationAvailabilityBean.class));
	}

	ApplicationAvailabilityBean(Log logger) {
		this.logger = logger;
	}

	@Override
	public <S extends AvailabilityState> S getState(Class<S> stateType, S defaultState) {
		Assert.notNull(stateType, "'stateType' must not be null");
		Assert.notNull(defaultState, "'defaultState' must not be null");
		S state = getState(stateType);
		return (state != null) ? state : defaultState;
	}

	@Override
	public <S extends AvailabilityState> @Nullable S getState(Class<S> stateType) {
		AvailabilityChangeEvent<S> event = getLastChangeEvent(stateType);
		return (event != null) ? event.getState() : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends AvailabilityState> @Nullable AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType) {
		return (AvailabilityChangeEvent<S>) this.events.get(stateType);
	}

	@Override
	public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
		Class<? extends AvailabilityState> type = getStateType(event.getState());
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(getLogMessage(type, event));
		}
		this.events.put(type, event);
	}

	private <S extends AvailabilityState> Object getLogMessage(Class<S> type, AvailabilityChangeEvent<?> event) {
		AvailabilityChangeEvent<S> lastChangeEvent = getLastChangeEvent(type);
		StringBuilder message = new StringBuilder(
				"Application availability state " + type.getSimpleName() + " changed");
		message.append((lastChangeEvent != null) ? " from " + lastChangeEvent.getState() : "");
		message.append(" to " + event.getState());
		message.append(getSourceDescription(event.getSource()));
		return message;
	}

	private String getSourceDescription(@Nullable Object source) {
		if (source == null || source instanceof ApplicationEventPublisher) {
			return "";
		}
		return ": " + ((source instanceof Throwable) ? source : source.getClass().getName());
	}

	@SuppressWarnings("unchecked")
	private Class<? extends AvailabilityState> getStateType(AvailabilityState state) {
		Class<?> type = (state instanceof Enum<?> enumState) ? enumState.getDeclaringClass() : state.getClass();
		return (Class<? extends AvailabilityState>) type;
	}

}
