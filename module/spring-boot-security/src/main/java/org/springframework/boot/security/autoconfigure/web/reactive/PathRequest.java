/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.reactive;

import org.springframework.boot.security.autoconfigure.web.StaticResourceLocation;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

/**
 * Factory that can be used to create a {@link ServerWebExchangeMatcher} for commonly used
 * paths.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
public final class PathRequest {

	private PathRequest() {
	}

	/**
	 * Returns a {@link StaticResourceRequest} that can be used to create a matcher for
	 * {@link StaticResourceLocation locations}.
	 * @return a {@link StaticResourceRequest}
	 */
	public static StaticResourceRequest toStaticResources() {
		return StaticResourceRequest.INSTANCE;
	}

}
