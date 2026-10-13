/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link GrpcChannelFactory} before it is fully initialized, in particular to tune its
 * configuration.
 *
 * @author Chris Bono
 * @since 4.1.0
 */
@FunctionalInterface
public interface GrpcChannelFactoryCustomizer {

	/**
	 * Customize the given {@link GrpcChannelFactory}.
	 * @param factory the factory to customize
	 */
	void customize(GrpcChannelFactory factory);

}
