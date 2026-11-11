/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.Operation;

/**
 * An operation on a web endpoint.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface WebOperation extends Operation {

	/**
	 * Returns the ID of the operation that uniquely identifies it within its endpoint.
	 * @return the ID
	 */
	String getId();

	/**
	 * Returns if the underlying operation is blocking.
	 * @return {@code true} if the operation is blocking
	 */
	boolean isBlocking();

	/**
	 * Returns the predicate for requests that can be handled by this operation.
	 * @return the predicate
	 */
	WebOperationRequestPredicate getRequestPredicate();

}
