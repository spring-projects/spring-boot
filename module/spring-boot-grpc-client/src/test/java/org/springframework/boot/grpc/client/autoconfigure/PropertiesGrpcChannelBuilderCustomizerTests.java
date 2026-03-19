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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.interceptor.DefaultDeadlineSetupClientInterceptor;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link PropertiesGrpcChannelBuilderCustomizer}.
 *
 * @author Phillip Webb
 */
class PropertiesGrpcChannelBuilderCustomizerTests {

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenHasMatchingChannel() {
		GrpcClientProperties properties = new GrpcClientProperties();
		properties.getChannel().put("test", createTestChannelProperties());
		GrpcChannelBuilderCustomizer<T> customizer = new PropertiesGrpcChannelBuilderCustomizer<>(properties);
		T builder = mock();
		customizer.customize("test", builder);
		assertMapped(builder);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenHasDefaultChannel() {
		GrpcClientProperties properties = new GrpcClientProperties();
		properties.getChannel().put("default", createTestChannelProperties());
		GrpcChannelBuilderCustomizer<T> customizer = new PropertiesGrpcChannelBuilderCustomizer<>(properties);
		T builder = mock();
		customizer.customize("test", builder);
		assertMapped(builder);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenHasNoMatchAndNoDefault() {
		GrpcClientProperties properties = new GrpcClientProperties();
		properties.getChannel().put("other", createTestChannelProperties());
		GrpcChannelBuilderCustomizer<T> customizer = new PropertiesGrpcChannelBuilderCustomizer<>(properties);
		T builder = mock();
		customizer.customize("test", builder);
		assertMappedStockDefaults(builder);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenChannelDoesNotSupportLoadBalancingDoesNotMapDefaultLoadBalancer() {
		assertNoLoadBalancerMappedBasedOnChannel("unix:test");
		assertNoLoadBalancerMappedBasedOnChannel("in-process:test");
	}

	private <T extends ManagedChannelBuilder<T>> void assertNoLoadBalancerMappedBasedOnChannel(String target) {
		T builder = getBuilder((channelProperties) -> {
			channelProperties.setTarget(target);
			channelProperties.getDefault().setLoadBalancingPolicy("testlbp");
		});
		then(builder).should(never()).defaultLoadBalancingPolicy(any());
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenTargetDoesNotSupportLoadBalancingDoesNotMapDefaultLoadBalancer() {
		GrpcChannelBuilderCustomizer<?> customizer = getCustomizer(
				(channelProperties) -> channelProperties.setTarget("static://localhost:1234"));
		assertNoLoadBalancerMappedBasedOnTarget(customizer, "unix:test");
		assertNoLoadBalancerMappedBasedOnTarget(customizer, "in-process:test");
	}

	private <T extends ManagedChannelBuilder<T>> void assertNoLoadBalancerMappedBasedOnTarget(
			GrpcChannelBuilderCustomizer<T> customizer, String target) {
		T builder = mock();
		customizer.customize(target, builder);
		then(builder).should(never()).defaultLoadBalancingPolicy(any());
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenMaxMessageSizeIsMinusOneUsesMaxValue() {
		T builder = getBuilder(
				(channelProperties) -> channelProperties.getInbound().getMessage().setMaxSize(DataSize.ofBytes(-1)));
		then(builder).should().maxInboundMessageSize(Integer.MAX_VALUE);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenMaxMessageSizeIsTooBigUsesMaxValue() {
		T builder = getBuilder((channelProperties) -> channelProperties.getInbound()
			.getMessage()
			.setMaxSize(DataSize.ofBytes((long) Integer.MAX_VALUE + 100)));
		then(builder).should().maxInboundMessageSize(Integer.MAX_VALUE);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenMaxMessageSizeIsNegativeAndNotMinusOneThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> getBuilder(
				(channelProperties) -> channelProperties.getInbound().getMessage().setMaxSize(DataSize.ofBytes(-2))))
			.withMessage("Unsupported max size value -2B");
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenMaxMetadataSizeIsMinusOneUsesMaxValue() {
		T builder = getBuilder(
				(channelProperties) -> channelProperties.getInbound().getMetadata().setMaxSize(DataSize.ofBytes(-1)));
		then(builder).should().maxInboundMetadataSize(Integer.MAX_VALUE);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenMaxMetadataSizeIsTooBigUsesMaxValue() {
		T builder = getBuilder((channelProperties) -> channelProperties.getInbound()
			.getMetadata()
			.setMaxSize(DataSize.ofBytes((long) Integer.MAX_VALUE + 100)));
		then(builder).should().maxInboundMetadataSize(Integer.MAX_VALUE);
	}

	@Test
	<T extends ManagedChannelBuilder<T>> void customizeWhenMaxMetadataSizeIsNegativeAndNotMinusOneThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> getBuilder(
				(channelProperties) -> channelProperties.getInbound().getMetadata().setMaxSize(DataSize.ofBytes(-2))))
			.withMessage("Unsupported max size value -2B");
	}

	private <T extends ManagedChannelBuilder<T>> T getBuilder(Consumer<Channel> setup) {
		GrpcChannelBuilderCustomizer<T> customizer = getCustomizer(setup);
		T builder = mock();
		customizer.customize("test", builder);
		return builder;
	}

	private <T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> getCustomizer(
			Consumer<Channel> setup) {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channelProperties = new Channel();
		setup.accept(channelProperties);
		properties.getChannel().put("test", channelProperties);
		GrpcChannelBuilderCustomizer<T> customizer = new PropertiesGrpcChannelBuilderCustomizer<>(properties);
		return customizer;
	}

	private Channel createTestChannelProperties() {
		Channel properties = new Channel();
		properties.setUserAgent("testua");
		properties.getInbound().getMessage().setMaxSize(DataSize.ofBytes(10));
		properties.getInbound().getMetadata().setMaxSize(DataSize.ofBytes(20));
		properties.getDefault().setDeadline(Duration.ofMinutes(5));
		properties.getDefault().setLoadBalancingPolicy("testlbp");
		properties.getIdle().setTimeout(Duration.ofMinutes(6));
		properties.getKeepalive().setTime(Duration.ofMinutes(7));
		properties.getKeepalive().setTimeout(Duration.ofMinutes(8));
		properties.getKeepalive().setWithoutCalls(true);
		return properties;
	}

	private <T extends ManagedChannelBuilder<T>> void assertMapped(T builder) {
		then(builder).should().userAgent("testua");
		then(builder).should().maxInboundMessageSize(10);
		then(builder).should().maxInboundMetadataSize(20);
		ArgumentCaptor<ClientInterceptor[]> interceptors = ArgumentCaptor.captor();
		then(builder).should().intercept(interceptors.capture());
		ClientInterceptor interceptor = interceptors.getValue()[0];
		assertThat(interceptor).isInstanceOf(DefaultDeadlineSetupClientInterceptor.class)
			.extracting("defaultDeadline")
			.isEqualTo(Duration.ofMinutes(5));
		then(builder).should().defaultLoadBalancingPolicy("testlbp");
		then(builder).should().idleTimeout(360000000000L, TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveTime(420000000000L, TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveTimeout(480000000000L, TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveWithoutCalls(true);
	}

	private <T extends ManagedChannelBuilder<T>> void assertMappedStockDefaults(T builder) {
		then(builder).should().maxInboundMessageSize(4194304);
		then(builder).should().maxInboundMetadataSize(8192);
		then(builder).should().defaultLoadBalancingPolicy("round_robin");
		then(builder).should().idleTimeout(20000000000L, TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveTime(300000000000L, TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveTimeout(20000000000L, TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveWithoutCalls(false);
		then(builder).shouldHaveNoMoreInteractions();
	}

}
