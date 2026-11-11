/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

/**
 * Redirect strategies support by HTTP clients.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public enum HttpRedirects {

	/**
	 * Follow redirects (if the underlying library has support).
	 */
	FOLLOW_WHEN_POSSIBLE,

	/**
	 * Follow redirects (fail if the underlying library has no support).
	 */
	FOLLOW,

	/**
	 * Don't follow redirects (fail if the underlying library has no support).
	 */
	DONT_FOLLOW

}
