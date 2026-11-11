/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

/**
 * An enumeration of HTTP methods supported by web endpoint operations.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public enum WebEndpointHttpMethod {

	/**
	 * An HTTP GET request.
	 */
	GET,

	/**
	 * An HTTP POST request.
	 */
	POST,

	/**
	 * An HTTP DELETE request.
	 */
	DELETE

}
