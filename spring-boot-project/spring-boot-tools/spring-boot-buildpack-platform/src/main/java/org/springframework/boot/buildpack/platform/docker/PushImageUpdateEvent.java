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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A {@link ProgressUpdateEvent} fired as an image is pushed to a registry.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public class PushImageUpdateEvent extends ImageProgressUpdateEvent {

	private final ErrorDetail errorDetail;

	/**
	 * Constructs a new PushImageUpdateEvent with the specified parameters.
	 * @param id the ID of the event
	 * @param status the status of the event
	 * @param progressDetail the progress detail of the event
	 * @param progress the progress of the event
	 * @param errorDetail the error detail of the event
	 */
	@JsonCreator
	public PushImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress,
			ErrorDetail errorDetail) {
		super(id, status, progressDetail, progress);
		this.errorDetail = errorDetail;
	}

	/**
	 * Returns the details of any error encountered during processing.
	 * @return the error
	 */
	public ErrorDetail getErrorDetail() {
		return this.errorDetail;
	}

	/**
	 * Details of an error embedded in a response stream.
	 */
	public static class ErrorDetail {

		private final String message;

		/**
		 * Constructs a new ErrorDetail object with the specified message.
		 * @param message the error message
		 */
		@JsonCreator
		public ErrorDetail(@JsonProperty("message") String message) {
			this.message = message;
		}

		/**
		 * Returns the message field from the error detail.
		 * @return the message
		 */
		public String getMessage() {
			return this.message;
		}

		/**
		 * Returns a string representation of the ErrorDetail object.
		 * @return the message of the ErrorDetail object
		 */
		@Override
		public String toString() {
			return this.message;
		}

	}

}
