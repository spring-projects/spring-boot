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

package org.springframework.boot.grpc.server.autoconfigure;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.grpc.ServerBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.util.unit.DataSize;

/**
 * Helper class used to map {@link GrpcServerProperties} to
 * {@link DefaultGrpcServerFactory}.
 *
 * @param <T> the type of server builder
 * @author Chris Bono
 */
class DefaultServerFactoryPropertyMapper<T extends ServerBuilder<T>> {

	private final GrpcServerProperties properties;

	DefaultServerFactoryPropertyMapper(GrpcServerProperties properties) {
		this.properties = properties;
	}

	/**
	 * Map the properties to the server factory's server builder.
	 * @param serverBuilder the builder
	 */
	void customizeServerBuilder(T serverBuilder) {
		PropertyMapper map = PropertyMapper.get();
		customizeKeepAlive(serverBuilder, map);
		customizeInboundLimits(serverBuilder, map);
	}

	/**
	 * Map the keep-alive properties to the server factory's server builder.
	 * @param serverBuilder the builder
	 * @param map the property mapper
	 */
	void customizeKeepAlive(T serverBuilder, PropertyMapper map) {
		GrpcServerProperties.KeepAlive keepAliveProps = this.properties.getKeepAlive();
		map.from(keepAliveProps.getTime()).to(durationProperty(serverBuilder::keepAliveTime));
		map.from(keepAliveProps.getTimeout()).to(durationProperty(serverBuilder::keepAliveTimeout));
		map.from(keepAliveProps.getMaxIdle()).to(durationProperty(serverBuilder::maxConnectionIdle));
		map.from(keepAliveProps.getMaxAge()).to(durationProperty(serverBuilder::maxConnectionAge));
		map.from(keepAliveProps.getMaxAgeGrace()).to(durationProperty(serverBuilder::maxConnectionAgeGrace));
		map.from(keepAliveProps.getPermitTime()).to(durationProperty(serverBuilder::permitKeepAliveTime));
		map.from(keepAliveProps.isPermitWithoutCalls()).to(serverBuilder::permitKeepAliveWithoutCalls);
	}

	/**
	 * Map the inbound limits properties to the server factory's server builder.
	 * @param serverBuilder the builder
	 * @param map the property mapper
	 */
	void customizeInboundLimits(T serverBuilder, PropertyMapper map) {
		map.from(this.properties.getMaxInboundMessageSize())
			.asInt(DataSize::toBytes)
			.to(serverBuilder::maxInboundMessageSize);
		map.from(this.properties.getMaxInboundMetadataSize())
			.asInt(DataSize::toBytes)
			.to(serverBuilder::maxInboundMetadataSize);
	}

	Consumer<Duration> durationProperty(BiConsumer<Long, TimeUnit> setter) {
		return (duration) -> setter.accept(duration.toNanos(), TimeUnit.NANOSECONDS);
	}

}
