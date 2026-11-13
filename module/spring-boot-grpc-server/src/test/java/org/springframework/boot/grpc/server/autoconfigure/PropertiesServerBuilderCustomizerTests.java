/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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
	void customizeWhenServletServerBuilder() {
		ServletServerBuilder builder = mock();
		PropertiesServerBuilderCustomizer<ServletServerBuilder> customizer = new PropertiesServerBuilderCustomizer<>(
				getProperties());
		customizer.customize(builder);
		assertExpectedMapping(builder, never());
	}

	@Test
	void customizeWhenMaxMessageSizeIsMinusOneUsesMaxValue() {
		NettyServerBuilder builder = getBuilder(
				(properties) -> properties.getInbound().getMessage().setMaxSize(DataSize.ofBytes(-1)));
		then(builder).should().maxInboundMessageSize(Integer.MAX_VALUE);
	}

	@Test
	void customizeWhenMaxMessageSizeIsTooBigUsesMaxValue() {
		NettyServerBuilder builder = getBuilder((properties) -> properties.getInbound()
			.getMessage()
			.setMaxSize(DataSize.ofBytes((long) Integer.MAX_VALUE + 100)));
		then(builder).should().maxInboundMessageSize(Integer.MAX_VALUE);
	}

	@Test
	void customizeWhenMaxMessageSizeIsNegativeAndNotMinusOneThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> getBuilder((properties) -> properties.getInbound().getMessage().setMaxSize(DataSize.ofBytes(-2))))
			.withMessage("Unsupported max size value -2B");
	}

	@Test
	void customizeWhenMaxMetadataSizeIsMinusOneUsesMaxValue() {
		NettyServerBuilder builder = getBuilder(
				(properties) -> properties.getInbound().getMetadata().setMaxSize(DataSize.ofBytes(-1)));
		then(builder).should().maxInboundMetadataSize(Integer.MAX_VALUE);
	}

	@Test
	void customizeWhenMaxMetadataSizeIsTooBigUsesMaxValue() {
		NettyServerBuilder builder = getBuilder((properties) -> properties.getInbound()
			.getMetadata()
			.setMaxSize(DataSize.ofBytes((long) Integer.MAX_VALUE + 100)));
		then(builder).should().maxInboundMetadataSize(Integer.MAX_VALUE);
	}

	@Test
	void customizeWhenMaxMetadataSizeIsNegativeAndNotMinusOneThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> getBuilder(
					(properties) -> properties.getInbound().getMetadata().setMaxSize(DataSize.ofBytes(-2))))
			.withMessage("Unsupported max size value -2B");
	}

	private NettyServerBuilder getBuilder(Consumer<GrpcServerProperties> setup) {
		GrpcServerProperties properties = new GrpcServerProperties();
		setup.accept(properties);
		NettyServerBuilder builder = mock();
		PropertiesServerBuilderCustomizer<NettyServerBuilder> customizer = new PropertiesServerBuilderCustomizer<>(
				properties);
		customizer.customize(builder);
		return builder;
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
