/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.observation.autoconfigure;

import java.util.List;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;

import org.springframework.util.Assert;

/**
 * Group of {@link ObservationHandler ObservationHandlers} that can be registered
 * together. The first group claiming membership of a handler is responsible for
 * registering it. Groups are {@link Comparable} so that they can be ordered against each
 * other.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface ObservationHandlerGroup extends Comparable<ObservationHandlerGroup> {

	/**
	 * Return if the given handler is a member of this group.
	 * @param handler the handler to check
	 * @return if the handler is a member
	 */
	default boolean isMember(ObservationHandler<?> handler) {
		return handlerType().isInstance(handler);
	}

	/**
	 * Register group members against the given {@link ObservationConfig}.
	 * @param config the config used to register members
	 * @param members the group members to register
	 */
	default void registerMembers(ObservationConfig config, List<ObservationHandler<?>> members) {
		config.observationHandler(new FirstMatchingCompositeObservationHandler(members));
	}

	@Override
	default int compareTo(ObservationHandlerGroup other) {
		return 0;
	}

	/**
	 * Return the primary type of handler that this group accepts.
	 * @return the accepted handler type
	 */
	Class<?> handlerType();

	/**
	 * Static factory method to create an {@link ObservationHandlerGroup} with members of
	 * the given handler type.
	 * @param <H> the handler type
	 * @param handlerType the handler type
	 * @return a new {@link ObservationHandlerGroup}
	 */
	static <H extends ObservationHandler<?>> ObservationHandlerGroup of(Class<H> handlerType) {
		Assert.notNull(handlerType, "'handlerType' must not be null");
		return () -> handlerType;
	}

}
