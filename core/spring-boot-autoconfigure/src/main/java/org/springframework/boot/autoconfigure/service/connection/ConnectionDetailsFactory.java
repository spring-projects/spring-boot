/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.service.connection;

import org.jspecify.annotations.Nullable;

/**
 * A factory to create {@link ConnectionDetails} from a given {@code source}.
 * Implementations should be registered in {@code META-INF/spring.factories}.
 *
 * @param <S> the source type accepted by the factory. Implementations are expected to
 * provide a valid {@code toString}.
 * @param <D> the type of {@link ConnectionDetails} produced by the factory
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface ConnectionDetailsFactory<S, D extends ConnectionDetails> {

	/**
	 * Get the {@link ConnectionDetails} from the given {@code source}. May return
	 * {@code null} if no details can be created.
	 * @param source the source
	 * @return the connection details or {@code null}
	 */
	@Nullable D getConnectionDetails(S source);

}
