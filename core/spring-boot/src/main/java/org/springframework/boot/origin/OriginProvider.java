/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.jspecify.annotations.Nullable;

/**
 * Interface to provide access to the origin of an item.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see Origin
 */
@FunctionalInterface
public interface OriginProvider {

	/**
	 * Return the source origin or {@code null} if the origin is not known.
	 * @return the origin or {@code null}
	 */
	@Nullable Origin getOrigin();

}
