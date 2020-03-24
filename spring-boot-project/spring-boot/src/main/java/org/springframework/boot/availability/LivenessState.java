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

import java.util.Objects;

/**
 * "Liveness" state of the application.
 * <p>
 * An application is considered live when it's running with a correct internal state.
 * "Liveness" failure means that the internal state of the application is broken and we
 * cannot recover from it. As a result, the platform should restart the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public final class LivenessState {

	private final Status status;

	LivenessState(Status status) {
		this.status = status;
	}

	public Status getStatus() {
		return this.status;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.status == ((LivenessState) obj).status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.status);
	}

	@Override
	public String toString() {
		return "LivenessState{" + "status=" + this.status + '}';
	}

	public static LivenessState broken() {
		return new LivenessState(Status.BROKEN);
	}

	public static LivenessState live() {
		return new LivenessState(Status.LIVE);
	}

	public enum Status {

		/**
		 * The application is running and its internal state is correct.
		 */
		LIVE,

		/**
		 * The internal state of the application is broken.
		 */
		BROKEN

	}

}
