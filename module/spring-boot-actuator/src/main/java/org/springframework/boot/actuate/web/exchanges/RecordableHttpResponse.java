/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.util.List;
import java.util.Map;

/**
 * The recordable parts of an HTTP response used when creating an {@link HttpExchange}.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 * @see RecordableHttpRequest
 */
public interface RecordableHttpResponse {

	/**
	 * The status of the response.
	 * @return the status
	 */
	int getStatus();

	/**
	 * Returns a modifiable copy of the headers of the response.
	 * @return the headers
	 */
	Map<String, List<String>> getHeaders();

}
