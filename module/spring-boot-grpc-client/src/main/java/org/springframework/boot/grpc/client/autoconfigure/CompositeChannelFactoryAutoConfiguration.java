/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import io.grpc.stub.AbstractStub;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.grpc.client.CompositeGrpcChannelFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a
 * {@link CompositeGrpcChannelFactory}.
 *
 * @author Chris Bono
 * @since 4.1.0
 */
@AutoConfiguration
@ConditionalOnClass({ AbstractStub.class, GrpcChannelBuilderCustomizer.class })
@ConditionalOnProperty(name = "spring.grpc.client.enabled", matchIfMissing = true)
@Conditional(CompositeChannelFactoryAutoConfiguration.MultipleNonPrimaryChannelFactoriesCondition.class)
public final class CompositeChannelFactoryAutoConfiguration {

	@Bean
	@Primary
	CompositeGrpcChannelFactory compositeChannelFactory(ObjectProvider<GrpcChannelFactory> channelFactoriesProvider) {
		return new CompositeGrpcChannelFactory(channelFactoriesProvider.orderedStream().toList());
	}

	static class MultipleNonPrimaryChannelFactoriesCondition extends NoneNestedConditions {

		MultipleNonPrimaryChannelFactoriesCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingBean(GrpcChannelFactory.class)
		static class NoChannelFactoryCondition {

		}

		@ConditionalOnSingleCandidate(GrpcChannelFactory.class)
		static class SingleInjectableChannelFactoryCondition {

		}

	}

}
