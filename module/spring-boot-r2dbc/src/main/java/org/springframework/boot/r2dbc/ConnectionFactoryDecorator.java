/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

/**
 * Decorator for {@link ConnectionFactory connection factories}.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 * @see ConnectionFactoryBuilder
 */
@FunctionalInterface
public interface ConnectionFactoryDecorator {

	/**
	 * Decorates the given {@link ConnectionFactory}.
	 * @param delegate the connection factory which should be decorated
	 * @return the decorated connection factory
	 */
	ConnectionFactory decorate(ConnectionFactory delegate);

}
