/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import org.jspecify.annotations.Nullable;

/**
 * An operation on an {@link ExposableEndpoint endpoint}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface Operation {

	/**
	 * Returns the {@link OperationType type} of the operation.
	 * @return the type
	 */
	OperationType getType();

	/**
	 * Invoke the underlying operation using the given {@code context}. Results intended
	 * to be returned in the body of the response should additionally implement
	 * {@link OperationResponseBody}.
	 * @param context the context to use when invoking the operation
	 * @return the result of the operation, may be {@code null}
	 */
	@Nullable Object invoke(InvocationContext context);

}
