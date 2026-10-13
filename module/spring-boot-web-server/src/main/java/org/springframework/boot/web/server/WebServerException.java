/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

import org.jspecify.annotations.Nullable;

/**
 * Exceptions thrown by a web server.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public class WebServerException extends RuntimeException {

	public WebServerException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

}
