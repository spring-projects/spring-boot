/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.context.annotation.Conditional;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;

/**
 * {@link Conditional @Conditional} that matches when no network gRPC server is found.
 * Concretely:
 * <ul>
 * <li>There are no {@link GrpcServletRegistration} beans.</li>
 * <li>There are no {@link GrpcServerFactory} beans (ignoring
 * {@link InProcessGrpcServerFactory} beans)</li>
 * </ul>
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(MissingNetworkGrpcServerCondition.class)
@interface ConditionalOnMissingNetworkGrpcServer {

}
