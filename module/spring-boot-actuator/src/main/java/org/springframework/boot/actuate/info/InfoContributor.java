/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

/**
 * Contributes additional info details.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@FunctionalInterface
public interface InfoContributor {

	/**
	 * Contributes additional details using the specified {@link Info.Builder Builder}.
	 * @param builder the builder to use
	 */
	void contribute(Info.Builder builder);

}
