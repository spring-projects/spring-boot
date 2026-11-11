/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The recordable parts of an HTTP request used when creating an {@link HttpExchange}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 * @see RecordableHttpResponse
 */
public interface RecordableHttpRequest {

	/**
	 * Returns the URI of the request.
	 * @return the URI
	 */
	URI getUri();

	/**
	 * Returns the remote address from which the request was sent, if available.
	 * @return the remote address or {@code null}
	 */
	@Nullable String getRemoteAddress();

	/**
	 * Returns the method (GET, POST, etc.) of the request.
	 * @return the method
	 */
	String getMethod();

	/**
	 * Returns a modifiable copy of the headers of the request.
	 * @return the headers
	 */
	Map<String, List<String>> getHeaders();

}
