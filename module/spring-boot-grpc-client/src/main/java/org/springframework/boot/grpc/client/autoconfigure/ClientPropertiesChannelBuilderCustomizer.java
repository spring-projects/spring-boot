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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.grpc.ManagedChannelBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.ChannelConfig;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.interceptor.DefaultDeadlineSetupClientInterceptor;
import org.springframework.util.unit.DataSize;

/**
 * A {@link GrpcChannelBuilderCustomizer} that maps {@link GrpcClientProperties client
 * properties} to a channel builder.
 *
 * @param <T> the type of the builder
 * @author David Syer
 * @author Chris Bono
 */
class ClientPropertiesChannelBuilderCustomizer<T extends ManagedChannelBuilder<T>>
		implements GrpcChannelBuilderCustomizer<T> {

	private final GrpcClientProperties properties;

	ClientPropertiesChannelBuilderCustomizer(GrpcClientProperties properties) {
		this.properties = properties;
	}

	@Override
	public void customize(String authority, T builder) {
		ChannelConfig channel = this.properties.getChannel(authority);
		PropertyMapper mapper = PropertyMapper.get();
		mapper.from(channel.getUserAgent()).to(builder::userAgent);
		if (!authority.startsWith("unix:") && !authority.startsWith("in-process:")) {
			mapper.from(channel.getDefaultLoadBalancingPolicy()).to(builder::defaultLoadBalancingPolicy);
		}
		mapper.from(channel.getMaxInboundMessageSize()).asInt(DataSize::toBytes).to(builder::maxInboundMessageSize);
		mapper.from(channel.getMaxInboundMetadataSize()).asInt(DataSize::toBytes).to(builder::maxInboundMetadataSize);
		mapper.from(channel.getKeepAliveTime()).to(durationProperty(builder::keepAliveTime));
		mapper.from(channel.getKeepAliveTimeout()).to(durationProperty(builder::keepAliveTimeout));
		mapper.from(channel.getIdleTimeout()).to(durationProperty(builder::idleTimeout));
		mapper.from(channel.isKeepAliveWithoutCalls()).to(builder::keepAliveWithoutCalls);
		Map<String, Object> defaultServiceConfig = channel.extractServiceConfig();
		if (channel.getHealth().isEnabled()) {
			String serviceNameToCheck = (channel.getHealth().getServiceName() != null)
					? channel.getHealth().getServiceName() : "";
			defaultServiceConfig.put("healthCheckConfig", Map.of("serviceName", serviceNameToCheck));
		}
		if (!defaultServiceConfig.isEmpty()) {
			builder.defaultServiceConfig(defaultServiceConfig);
		}
		if (channel.getDefaultDeadline() != null && channel.getDefaultDeadline().toMillis() > 0L) {
			builder.intercept(new DefaultDeadlineSetupClientInterceptor(channel.getDefaultDeadline()));
		}
	}

	Consumer<Duration> durationProperty(BiConsumer<Long, TimeUnit> setter) {
		return (duration) -> setter.accept(duration.toNanos(), TimeUnit.NANOSECONDS);
	}

}
