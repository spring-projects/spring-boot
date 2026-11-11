/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * A {@link ProgressUpdateEvent} fired as an image is pushed to a registry.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public class PushImageUpdateEvent extends ImageProgressUpdateEvent {

	private final @Nullable ErrorDetail errorDetail;

	@JsonCreator
	public PushImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress,
			@Nullable ErrorDetail errorDetail) {
		super(id, status, progressDetail, progress);
		this.errorDetail = errorDetail;
	}

	/**
	 * Returns the details of any error encountered during processing.
	 * @return the error
	 */
	public @Nullable ErrorDetail getErrorDetail() {
		return this.errorDetail;
	}

	/**
	 * Details of an error embedded in a response stream.
	 */
	public static class ErrorDetail {

		private final String message;

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

		@Override
		public String toString() {
			return this.message;
		}

	}

}
