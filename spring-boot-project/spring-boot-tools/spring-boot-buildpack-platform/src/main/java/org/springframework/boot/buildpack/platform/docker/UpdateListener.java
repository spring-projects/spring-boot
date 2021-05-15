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

package org.springframework.boot.buildpack.platform.docker;

/**
 * Listener for update events published from the {@link DockerApi}.
 *
 * @param <E> the update event type
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface UpdateListener<E extends UpdateEvent> {

	/**
	 * A no-op update listener.
	 * @see #none()
	 */
	UpdateListener<UpdateEvent> NONE = (event) -> {
	};

	/**
	 * Called when the operation starts.
	 */
	default void onStart() {
	}

	/**
	 * Called when an update event is available.
	 * @param event the update event
	 */
	void onUpdate(E event);

	/**
	 * Called when the operation finishes (with or without error).
	 */
	default void onFinish() {
	}

	/**
	 * A no-op update listener that does nothing.
	 * @param <E> the event type
	 * @return a no-op update listener
	 */
	@SuppressWarnings("unchecked")
	static <E extends UpdateEvent> UpdateListener<E> none() {
		return (UpdateListener<E>) NONE;
	}

}
