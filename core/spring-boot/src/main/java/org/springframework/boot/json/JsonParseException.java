/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import org.jspecify.annotations.Nullable;

/**
 * {@link IllegalArgumentException} thrown when source JSON is invalid.
 *
 * @author Anton Telechev
 * @author Phillip Webb
 * @since 2.0.1
 */
public class JsonParseException extends IllegalArgumentException {

	public JsonParseException() {
		this(null);
	}

	public JsonParseException(@Nullable Throwable cause) {
		super("Cannot parse JSON", cause);
	}

}
