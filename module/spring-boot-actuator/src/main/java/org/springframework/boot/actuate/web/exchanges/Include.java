/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Include options for HTTP exchanges.
 *
 * @author Wallace Wadge
 * @author Emily Tsanova
 * @author Joseph Beeton
 * @since 3.0.0
 */
public enum Include {

	/**
	 * Include request headers.
	 */
	REQUEST_HEADERS,

	/**
	 * Include the remote address from the request.
	 */
	REMOTE_ADDRESS,

	/**
	 * Include "Cookie" header (if any) in request headers and "Set-Cookie" (if any) in
	 * response headers.
	 */
	COOKIE_HEADERS,

	/**
	 * Include authorization header (if any).
	 */
	AUTHORIZATION_HEADER,

	/**
	 * Include response headers.
	 */
	RESPONSE_HEADERS,

	/**
	 * Include the principal.
	 */
	PRINCIPAL,

	/**
	 * Include the session ID.
	 */
	SESSION_ID,

	/**
	 * Include the time taken to service the request.
	 */
	TIME_TAKEN;

	private static final Set<Include> DEFAULT_INCLUDES;

	static {
		Set<Include> defaultIncludes = new LinkedHashSet<>();
		defaultIncludes.add(Include.TIME_TAKEN);
		defaultIncludes.add(Include.REQUEST_HEADERS);
		defaultIncludes.add(Include.RESPONSE_HEADERS);
		DEFAULT_INCLUDES = Collections.unmodifiableSet(defaultIncludes);
	}

	/**
	 * Return the default {@link Include}.
	 * @return the default include.
	 */
	public static Set<Include> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

}
