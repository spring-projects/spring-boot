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
import java.util.function.Function;
import java.util.function.Supplier;

import io.grpc.ServerBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.util.unit.DataSize;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultServerFactoryPropertyMapper},
 * {@link NettyServerFactoryPropertyMapper}, and
 * {@link ShadedNettyServerFactoryPropertyMapper}.
 *
 * @author Chris Bono
 */
class ServerFactoryPropertyMappersTests {

	@Test
	void customizeShadedNettyServerBuilder() {
		io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder builder = mock();
		customizeServerBuilder(ShadedNettyServerFactoryPropertyMapper::new, () -> builder);
	}

	@Test
	void customizeNettyServerBuilder() {
		io.grpc.netty.NettyServerBuilder builder = mock();
		customizeServerBuilder(NettyServerFactoryPropertyMapper::new, () -> builder);
	}

	@Test
	<T extends ServerBuilder<T>> void customizeBaseServerBuilder() {
		T builder = mock();
		customizeServerBuilder(DefaultServerFactoryPropertyMapper::new, () -> builder);
	}

	private <T extends ServerBuilder<T>, X extends DefaultServerFactoryPropertyMapper<T>> void customizeServerBuilder(
			Function<GrpcServerProperties, X> mapperFactory, Supplier<T> mockBuilderToCustomize) {
		GrpcServerProperties properties = new GrpcServerProperties();
		properties.getKeepAlive().setTime(Duration.ofHours(1));
		properties.getKeepAlive().setTimeout(Duration.ofSeconds(10));
		properties.getKeepAlive().setMaxIdle(Duration.ofHours(2));
		properties.getKeepAlive().setMaxAge(Duration.ofHours(3));
		properties.getKeepAlive().setMaxAgeGrace(Duration.ofSeconds(45));
		properties.getKeepAlive().setPermitTime(Duration.ofMinutes(7));
		properties.getKeepAlive().setPermitWithoutCalls(true);
		properties.setMaxInboundMessageSize(DataSize.ofMegabytes(333));
		properties.setMaxInboundMetadataSize(DataSize.ofKilobytes(111));
		X mapper = mapperFactory.apply(properties);
		T builder = mockBuilderToCustomize.get();
		mapper.customizeServerBuilder(builder);
		then(builder).should().keepAliveTime(Duration.ofHours(1).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should().keepAliveTimeout(Duration.ofSeconds(10).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should().maxConnectionIdle(Duration.ofHours(2).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should().maxConnectionAge(Duration.ofHours(3).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should().maxConnectionAgeGrace(Duration.ofSeconds(45).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should().permitKeepAliveTime(Duration.ofMinutes(7).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should().permitKeepAliveWithoutCalls(true);
		then(builder).should().maxInboundMessageSize(Math.toIntExact(DataSize.ofMegabytes(333).toBytes()));
		then(builder).should().maxInboundMetadataSize(Math.toIntExact(DataSize.ofKilobytes(111).toBytes()));
	}

}
