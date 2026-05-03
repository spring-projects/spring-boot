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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerBuilderCustomizers}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcServerBuilderCustomizersTests {

	@Test
	void applyWhenEmptyCustomizersDoesNothing() {
		ServerBuilder<?> serverBuilder = mock(ServerBuilder.class);
		new GrpcServerBuilderCustomizers(Collections.emptyList()).apply(serverBuilder);
		then(serverBuilder).shouldHaveNoInteractions();
	}

	@Test
	void applyWhenSimpleCustomizer() {
		GrpcServerBuilderCustomizers customizers = new GrpcServerBuilderCustomizers(
				List.of(new SimpleServerBuilderCustomizer()));
		NettyServerBuilder serverBuilder = mock(NettyServerBuilder.class);
		customizers.apply(serverBuilder);
		then(serverBuilder).should().maxConnectionAge(100L, TimeUnit.SECONDS);
	}

	@Test
	void applyWhenGenericCustomizersRespectsGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestNettyServerBuilderCustomizer());
		list.add(new TestShadedNettyServerBuilderCustomizer());
		GrpcServerBuilderCustomizers customizers = new GrpcServerBuilderCustomizers(list);
		customizers.apply(mock(ServerBuilder.class));
		assertThat(list.get(0).getCount()).isOne();
		assertThat(list.get(1).getCount()).isZero();
		assertThat(list.get(2).getCount()).isZero();
		customizers.apply(mock(NettyServerBuilder.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isZero();
		customizers.apply(mock(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class));
		assertThat(list.get(0).getCount()).isEqualTo(3);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isOne();
	}

	@Test
	void applyWhenHasInjectedBeans() {
		CompressorRegistry compressorRegistry = mock();
		DecompressorRegistry decompressorRegistry = mock();
		GrpcServerExecutorProvider executorProvider = mock();
		Executor executor = mock();
		given(executorProvider.getExecutor()).willReturn(executor);
		GrpcServerProperties properties = new GrpcServerProperties();
		properties.getInbound().getMessage().setMaxSize(DataSize.ofMegabytes(10));
		GrpcServerBuilderCustomizers customizers = new GrpcServerBuilderCustomizers(properties, compressorRegistry,
				decompressorRegistry, executorProvider, List.of(new SimpleServerBuilderCustomizer()));
		NettyServerBuilder serverBuilder = mock(NettyServerBuilder.class);
		customizers.apply(serverBuilder);
		InOrder ordered = Mockito.inOrder(serverBuilder);
		then(serverBuilder).should(ordered).compressorRegistry(compressorRegistry);
		then(serverBuilder).should(ordered).decompressorRegistry(decompressorRegistry);
		then(serverBuilder).should().executor(executor);
		then(serverBuilder).should(ordered).maxConnectionAge(100L, TimeUnit.SECONDS);
	}

	/**
	 * Test customizer that will match {@link NettyServerBuilder} and apply a simple
	 * customization.
	 */
	static class SimpleServerBuilderCustomizer implements ServerBuilderCustomizer<NettyServerBuilder> {

		@Override
		public void customize(NettyServerBuilder serverBuilder) {
			serverBuilder.maxConnectionAge(100, TimeUnit.SECONDS);
		}

	}

	/**
	 * Test customizer that will match all {@link ServerBuilderCustomizer}.
	 *
	 * @param <T> the builder type
	 */
	static class TestCustomizer<T extends ServerBuilder<T>> implements ServerBuilderCustomizer<T> {

		private int count;

		@Override
		public void customize(T serverBuilder) {
			this.count++;
		}

		int getCount() {
			return this.count;
		}

	}

	/**
	 * Test customizer that will match only {@link NettyServerBuilder}.
	 */
	static class TestNettyServerBuilderCustomizer extends TestCustomizer<NettyServerBuilder> {

	}

	/**
	 * Test customizer that will match only
	 * {@link io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder}.
	 */
	static class TestShadedNettyServerBuilderCustomizer
			extends TestCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder> {

	}

}
