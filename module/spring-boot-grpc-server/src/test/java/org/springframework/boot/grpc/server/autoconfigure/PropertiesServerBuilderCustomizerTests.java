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

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import org.springframework.util.unit.DataSize;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link PropertiesServerBuilderCustomizer}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class PropertiesServerBuilderCustomizerTests {

	@Test
	void customizeWhenNettyServerBuilder() {
		NettyServerBuilder builder = mock();
		PropertiesServerBuilderCustomizer<NettyServerBuilder> customizer = new PropertiesServerBuilderCustomizer<>(
				getProperties());
		customizer.customize(builder);
		assertExpectedMapping(builder, atLeastOnce());
	}

	@Test
	void customizeWhenInProcessServerBuilder() {
		InProcessServerBuilder builder = mock();
		PropertiesServerBuilderCustomizer<InProcessServerBuilder> customizer = new PropertiesServerBuilderCustomizer<>(
				getProperties());
		customizer.customize(builder);
		assertExpectedMapping(builder, never());
	}

	@Test
	void customizerWhenServletServerBuilder() {
		ServletServerBuilder builder = mock();
		PropertiesServerBuilderCustomizer<ServletServerBuilder> customizer = new PropertiesServerBuilderCustomizer<>(
				getProperties());
		customizer.customize(builder);
		assertExpectedMapping(builder, never());
	}

	private GrpcServerProperties getProperties() {
		GrpcServerProperties properties = new GrpcServerProperties();
		properties.getInbound().getMessage().setMaxSize(DataSize.ofMegabytes(333));
		properties.getInbound().getMetadata().setMaxSize(DataSize.ofKilobytes(111));
		properties.getKeepalive().setTime(Duration.ofHours(1));
		properties.getKeepalive().setTimeout(Duration.ofSeconds(10));
		properties.getKeepalive().getConnection().setMaxIdleTime(Duration.ofHours(2));
		properties.getKeepalive().getConnection().setMaxAge(Duration.ofHours(3));
		properties.getKeepalive().getConnection().setGracePeriod(Duration.ofSeconds(45));
		properties.getKeepalive().getPermit().setTime(Duration.ofMinutes(7));
		properties.getKeepalive().getPermit().setWithoutCalls(true);
		return properties;
	}

	private void assertExpectedMapping(ServerBuilder<?> builder, VerificationMode keepAliveMode) {
		then(builder).should().maxInboundMessageSize(Math.toIntExact(DataSize.ofMegabytes(333).toBytes()));
		then(builder).should().maxInboundMetadataSize(Math.toIntExact(DataSize.ofKilobytes(111).toBytes()));
		then(builder).should(keepAliveMode).keepAliveTime(Duration.ofHours(1).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should(keepAliveMode).keepAliveTimeout(Duration.ofSeconds(10).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should(keepAliveMode).maxConnectionIdle(Duration.ofHours(2).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should(keepAliveMode).maxConnectionAge(Duration.ofHours(3).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should(keepAliveMode)
			.maxConnectionAgeGrace(Duration.ofSeconds(45).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should(keepAliveMode).permitKeepAliveTime(Duration.ofMinutes(7).toNanos(), TimeUnit.NANOSECONDS);
		then(builder).should(keepAliveMode).permitKeepAliveWithoutCalls(true);
	}

}
