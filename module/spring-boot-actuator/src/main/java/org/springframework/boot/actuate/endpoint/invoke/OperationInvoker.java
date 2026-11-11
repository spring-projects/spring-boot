/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.InvocationContext;

/**
 * Interface to perform an operation invocation.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface OperationInvoker {

	/**
	 * Invoke the underlying operation using the given {@code context}.
	 * @param context the context to use to invoke the operation
	 * @return the result of the operation, may be {@code null}
	 * @throws MissingParametersException if parameters are missing
	 */
	@Nullable Object invoke(InvocationContext context) throws MissingParametersException;

}
