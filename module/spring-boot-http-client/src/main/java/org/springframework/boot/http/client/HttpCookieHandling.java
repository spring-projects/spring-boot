/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

/**
 * Cookie handling strategies supported by HTTP clients.
 *
 * @author Apoorv Darshan
 * @since 4.1.0
 */
public enum HttpCookieHandling {

	/**
	 * Enable cookie handling (if the underlying library has support). When enabled,
	 * cookies received in a response are stored and automatically included in subsequent
	 * matching requests.
	 */
	ENABLE_WHEN_POSSIBLE,

	/**
	 * Enable cookie handling (fail if the underlying library has no support). When
	 * enabled, cookies received in a response are stored and automatically included in
	 * subsequent matching requests.
	 */
	ENABLE,

	/**
	 * Disable cookie handling (fail if the underlying library has no support).
	 */
	DISABLE

}
