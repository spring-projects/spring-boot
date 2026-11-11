/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link DefaultCookieSerializer} configuration.
 *
 * @author Vedran Pavic
 * @since 4.0.0
 */
@FunctionalInterface
public interface DefaultCookieSerializerCustomizer {

	/**
	 * Customize the cookie serializer.
	 * @param cookieSerializer the {@code DefaultCookieSerializer} to customize
	 */
	void customize(DefaultCookieSerializer cookieSerializer);

}
