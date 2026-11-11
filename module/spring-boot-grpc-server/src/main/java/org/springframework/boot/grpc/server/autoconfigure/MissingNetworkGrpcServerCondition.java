/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.context.annotation.Condition;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;

/**
 * {@link Condition} that matches when no network gRPC server is found.
 *
 * @author Phillip Webb
 */
class MissingNetworkGrpcServerCondition extends AllNestedConditions {

	MissingNetworkGrpcServerCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnMissingBean(GrpcServletRegistration.class)
	static class MissingGrpcServletRegistrationBean {

	}

	@ConditionalOnMissingBean(value = GrpcServerFactory.class, ignored = InProcessGrpcServerFactory.class)
	static class MissingGrpcFactoryBean {

	}

}
