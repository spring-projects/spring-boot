/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * Provides access to the configured session timeout. The timeout is available
 * irrespective of the deployment type:
 * <ul>
 * <li>a servlet web application
 * <ul>
 * <li>using an embedded web server
 * <li>deployed as a war file
 * </ul>
 * <li>a reactive web application
 * </ul>
 *
 * @author Andy Wilkinson
 * @since 4.0.1
 */
@FunctionalInterface
public interface SessionTimeout {

	/**
	 * Returns the session timeout or {@code null} if no timeout has been configured.
	 * @return the timeout or {@code null}
	 */
	@Nullable Duration getTimeout();

}
