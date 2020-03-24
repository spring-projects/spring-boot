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

import org.springframework.context.ApplicationEvent;

/**
 * {@link ApplicationEvent} sent when the {@link LivenessState} of the application
 * changes.
 * <p>
 * Any application component can send such events to update the state of the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class LivenessStateChangedEvent extends ApplicationEvent {

	private final String cause;

	LivenessStateChangedEvent(LivenessState state, String cause) {
		super(state);
		this.cause = cause;
	}

	public LivenessState getLivenessState() {
		return (LivenessState) getSource();
	}

	/**
	 * Create a new {@code ApplicationEvent} signaling that the {@link LivenessState} is
	 * live.
	 * @param cause the cause of the live internal state of the application
	 * @return the application event
	 */
	public static LivenessStateChangedEvent live(String cause) {
		return new LivenessStateChangedEvent(LivenessState.LIVE, cause);
	}

	/**
	 * Create a new {@code ApplicationEvent} signaling that the {@link LivenessState} is
	 * broken.
	 * @param cause the cause of the broken internal state of the application
	 * @return the application event
	 */
	public static LivenessStateChangedEvent broken(String cause) {
		return new LivenessStateChangedEvent(LivenessState.BROKEN, cause);
	}

	/**
	 * Create a new {@code ApplicationEvent} signaling that the {@link LivenessState} is
	 * broken.
	 * @param throwable the exception that caused the broken internal state of the
	 * application
	 * @return the application event
	 */
	public static LivenessStateChangedEvent broken(Throwable throwable) {
		return new LivenessStateChangedEvent(LivenessState.BROKEN, throwable.getMessage());
	}

}
