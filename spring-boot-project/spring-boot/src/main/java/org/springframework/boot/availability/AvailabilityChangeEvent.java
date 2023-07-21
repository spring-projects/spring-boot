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
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * {@link ApplicationEvent} sent when the {@link AvailabilityState} of the application
 * changes.
 * <p>
 * Any application component can send such events to update the state of the application.
 *
 * @param <S> the availability state type
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.3.0
 */
public class AvailabilityChangeEvent<S extends AvailabilityState> extends PayloadApplicationEvent<S> {

	/**
	 * Create a new {@link AvailabilityChangeEvent} instance.
	 * @param source the source of the event
	 * @param state the availability state (never {@code null})
	 */
	public AvailabilityChangeEvent(Object source, S state) {
		super(source, state);
	}

	/**
	 * Return the changed availability state.
	 * @return the availability state
	 */
	public S getState() {
		return getPayload();
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), getStateType());
	}

	private Class<?> getStateType() {
		S state = getState();
		if (state instanceof Enum) {
			return ((Enum<?>) state).getDeclaringClass();
		}
		return state.getClass();
	}

	/**
	 * Convenience method that can be used to publish an {@link AvailabilityChangeEvent}
	 * to the given application context.
	 * @param <S> the availability state type
	 * @param context the context used to publish the event
	 * @param state the changed availability state
	 */
	public static <S extends AvailabilityState> void publish(ApplicationContext context, S state) {
		Assert.notNull(context, "Context must not be null");
		publish(context, context, state);
	}

	/**
	 * Convenience method that can be used to publish an {@link AvailabilityChangeEvent}
	 * to the given application context.
	 * @param <S> the availability state type
	 * @param publisher the publisher used to publish the event
	 * @param source the source of the event
	 * @param state the changed availability state
	 */
	public static <S extends AvailabilityState> void publish(ApplicationEventPublisher publisher, Object source,
			S state) {
		Assert.notNull(publisher, "Publisher must not be null");
		publisher.publishEvent(new AvailabilityChangeEvent<>(source, state));
	}

}
