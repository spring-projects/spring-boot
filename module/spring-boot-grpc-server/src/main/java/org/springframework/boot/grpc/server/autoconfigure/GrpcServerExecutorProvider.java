/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.concurrent.Executor;

/**
 * Strategy interface to determine the {@link Executor} to use for the gRPC server.
 *
 * @author Chris Bono
 * @since 4.1.0
 */
@FunctionalInterface
public interface GrpcServerExecutorProvider {

	/**
	 * Returns a {@link Executor} for the gRPC server, if it needs to be customized.
	 * @return the executor to use for the gRPC server
	 */
	Executor getExecutor();

}
