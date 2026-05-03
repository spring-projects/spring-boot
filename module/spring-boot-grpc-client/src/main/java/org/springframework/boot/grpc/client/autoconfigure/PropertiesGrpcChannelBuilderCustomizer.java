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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.grpc.ManagedChannelBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.interceptor.DefaultDeadlineSetupClientInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;

/**
 * {@link GrpcChannelBuilderCustomizer} that maps {@link GrpcClientProperties} to a
 * {@link ManagedChannelBuilder}.
 *
 * @param <T> the type of the builder
 * @param properties the properties to map
 * @author David Syer
 * @author Chris Bono
 * @author Phillip Webb
 */
record PropertiesGrpcChannelBuilderCustomizer<T extends ManagedChannelBuilder<T>>(
		GrpcClientProperties properties) implements GrpcChannelBuilderCustomizer<T> {

	private static final Channel STOCK_DEFAULT_CHANNEL = new Channel();

	@Override
	public void customize(String target, T builder) {
		Channel channel = getChannel(target);
		PropertyMapper map = PropertyMapper.get();
		map.from(channel::getUserAgent).to(builder::userAgent);
		map.from(channel.getInbound().getMessage()::getMaxSize).asInt(this::maxSize).to(builder::maxInboundMessageSize);
		map.from(channel.getInbound().getMetadata()::getMaxSize)
			.asInt(this::maxSize)
			.to(builder::maxInboundMetadataSize);
		map.from(channel.getDefault()::getDeadline)
			.when((deadline) -> deadline.toMillis() > 0L)
			.as(DefaultDeadlineSetupClientInterceptor::new)
			.to(builder::intercept);
		map.from(channel.getDefault()::getLoadBalancingPolicy)
			.when((policy) -> supportsLoadBalancing(target, channel))
			.to(builder::defaultLoadBalancingPolicy);
		map.from(channel.getIdle()::getTimeout).to(durationProperty(builder::idleTimeout));
		map.from(channel.getKeepalive()::getTime).to(durationProperty(builder::keepAliveTime));
		map.from(channel.getKeepalive()::getTimeout).to(durationProperty(builder::keepAliveTimeout));
		map.from(channel.getKeepalive()::isWithoutCalls).to(builder::keepAliveWithoutCalls);
	}

	private Channel getChannel(String target) {
		Channel channel = this.properties.getChannel().get(target);
		channel = (channel != null) ? channel : this.properties.getChannel().get("default");
		return (channel != null) ? channel : STOCK_DEFAULT_CHANNEL;
	}

	private boolean supportsLoadBalancing(String target, Channel channel) {
		return !(isUnixOrInProcessTarget(target) || isUnixOrInProcessTarget(channel.getTarget()));
	}

	private boolean isUnixOrInProcessTarget(String target) {
		return target.startsWith("unix:") || target.startsWith("in-process:");
	}

	Consumer<Duration> durationProperty(BiConsumer<Long, TimeUnit> setter) {
		return (duration) -> setter.accept(duration.toNanos(), TimeUnit.NANOSECONDS);
	}

	private int maxSize(DataSize maxSize) {
		long bytes = maxSize.toBytes();
		Assert.state(bytes >= 0 || bytes == -1, () -> "Unsupported max size value " + maxSize);
		return (bytes >= 0 && bytes <= Integer.MAX_VALUE) ? (int) bytes : Integer.MAX_VALUE;
	}

}
