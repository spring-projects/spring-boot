/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;

/**
 * Supplier that can safely throw {@link IOException IO exceptions}.
 *
 * @param <T> the supplied type
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface IOSupplier<T> {

	/**
	 * Gets the supplied value.
	 * @return the supplied value
	 * @throws IOException on IO error
	 */
	T get() throws IOException;

}
