/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;

/**
 * Consumer that can safely throw {@link IOException IO exceptions}.
 *
 * @param <T> the consumed type
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface IOConsumer<T> {

	/**
	 * Performs this operation on the given argument.
	 * @param t the instance to consume
	 * @throws IOException on IO error
	 */
	void accept(T t) throws IOException;

}
