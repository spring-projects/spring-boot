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

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ChannelBuilderCustomizers}.
 *
 * @author Chris Bono
 */
class ChannelBuilderCustomizersTests {

	private static final String DEFAULT_TARGET = "localhost";

	@Test
	void customizeWithNullCustomizersShouldDoNothing() {
		ManagedChannelBuilder<?> channelBuilder = mock(ManagedChannelBuilder.class);
		new ChannelBuilderCustomizers(null).customize(DEFAULT_TARGET, channelBuilder);
		then(channelBuilder).shouldHaveNoInteractions();
	}

	@Test
	void customizeSimpleChannelBuilder() {
		ChannelBuilderCustomizers customizers = new ChannelBuilderCustomizers(
				List.of(new SimpleChannelBuilderCustomizer()));
		NettyChannelBuilder channelBuilder = mock(NettyChannelBuilder.class);
		customizers.customize(DEFAULT_TARGET, channelBuilder);
		then(channelBuilder).should().flowControlWindow(100);
	}

	@Test
	void customizeShouldCheckGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestNettyChannelBuilderCustomizer());
		list.add(new TestShadedNettyChannelBuilderCustomizer());
		ChannelBuilderCustomizers customizers = new ChannelBuilderCustomizers(list);

		customizers.customize(DEFAULT_TARGET, mock(ManagedChannelBuilder.class));
		assertThat(list.get(0).getCount()).isOne();
		assertThat(list.get(1).getCount()).isZero();
		assertThat(list.get(2).getCount()).isZero();

		customizers.customize(DEFAULT_TARGET, mock(NettyChannelBuilder.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isZero();

		customizers.customize(DEFAULT_TARGET, mock(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class));
		assertThat(list.get(0).getCount()).isEqualTo(3);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isOne();
	}

	static class SimpleChannelBuilderCustomizer implements GrpcChannelBuilderCustomizer<NettyChannelBuilder> {

		@Override
		public void customize(String target, NettyChannelBuilder channelBuilder) {
			channelBuilder.flowControlWindow(100);
		}

	}

	/**
	 * Test customizer that will match any {@link GrpcChannelBuilderCustomizer}.
	 */
	static class TestCustomizer<T extends ManagedChannelBuilder<T>> implements GrpcChannelBuilderCustomizer<T> {

		private int count;

		@Override
		public void customize(String target, T channelBuilder) {
			this.count++;
		}

		int getCount() {
			return this.count;
		}

	}

	/**
	 * Test customizer that will match only {@link NettyChannelBuilder}.
	 */
	static class TestNettyChannelBuilderCustomizer extends TestCustomizer<NettyChannelBuilder> {

	}

	/**
	 * Test customizer that will match only
	 * {@link io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder}.
	 */
	static class TestShadedNettyChannelBuilderCustomizer
			extends TestCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder> {

	}

}
