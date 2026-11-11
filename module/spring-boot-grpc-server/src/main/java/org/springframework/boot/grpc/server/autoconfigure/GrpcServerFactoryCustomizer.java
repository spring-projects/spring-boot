/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import org.springframework.grpc.server.GrpcServerFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link GrpcServerFactory server factory} before it is fully initialized.
 *
 * @author Chris Bono
 * @since 4.1.0
 */
@FunctionalInterface
public interface GrpcServerFactoryCustomizer {

	/**
	 * Customize the given {@link GrpcServerFactory}.
	 * @param serverFactory the server factory to customize
	 */
	void customize(GrpcServerFactory serverFactory);

}
