/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;

/**
 * BiConsumer that can safely throw {@link IOException IO exceptions}.
 *
 * @param <T> the first consumed type
 * @param <U> the second consumed type
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface IOBiConsumer<T, U> {

	/**
	 * Performs this operation on the given argument.
	 * @param t the first instance to consume
	 * @param u the second instance to consumer
	 * @throws IOException on IO error
	 */
	void accept(T t, U u) throws IOException;

}
