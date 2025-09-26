/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.grpc.server.autoconfigure.exception;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnGrpcServerEnabled;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnSpringGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.exception.CompositeGrpcExceptionHandler;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side exception
 * handling.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnSpringGrpc
@ConditionalOnGrpcServerEnabled("exception-handler")
@ConditionalOnBean(GrpcExceptionHandler.class)
@ConditionalOnMissingBean(GrpcExceptionHandlerInterceptor.class)
public final class GrpcExceptionHandlerAutoConfiguration {

	@GlobalServerInterceptor
	@Bean
	GrpcExceptionHandlerInterceptor globalExceptionHandlerInterceptor(
			ObjectProvider<GrpcExceptionHandler> exceptionHandler) {
		return new GrpcExceptionHandlerInterceptor(new CompositeGrpcExceptionHandler(
				exceptionHandler.orderedStream().toArray(GrpcExceptionHandler[]::new)));
	}

}
