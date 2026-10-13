/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * A message returned from the Docker API.
 *
 * @author Scott Frederick
 * @since 2.3.1
 */
public class Message {

	private final @Nullable String message;

	@JsonCreator
	Message(@JsonProperty("message") @Nullable String message) {
		this.message = message;
	}

	/**
	 * Return the message contained in the response.
	 * @return the message
	 */
	public @Nullable String getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return (this.message == null) ? "<null>" : this.message;
	}

}
