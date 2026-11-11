/*
 * Copyright 2012-present the original author or authors.
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
