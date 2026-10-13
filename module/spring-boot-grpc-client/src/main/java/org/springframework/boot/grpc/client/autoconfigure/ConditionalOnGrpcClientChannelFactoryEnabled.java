/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when
 * {@code spring.grpc.client.channelfactory.enabled} is {@code true} or missing.
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnBooleanProperty(name = "spring.grpc.client.channelfactory.enabled", matchIfMissing = true)
@interface ConditionalOnGrpcClientChannelFactoryEnabled {

}
