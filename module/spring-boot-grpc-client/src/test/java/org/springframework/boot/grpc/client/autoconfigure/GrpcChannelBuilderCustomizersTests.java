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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.boot.grpc.client.autoconfigure.ServiceConfig.HealthCheckConfig;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link GrpcChannelBuilderCustomizers}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcChannelBuilderCustomizersTests {

	@Test
	void applyWhenHasProperties() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.setUserAgent("spring-boot");
		properties.getChannel().put("target", channel);
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		then(builder).should().userAgent("spring-boot");
	}

	@Test
	void applyWhenHasCompressorRegistry() {
		CompressorRegistry compressorRegistry = mock();
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(null, compressorRegistry, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		then(builder).should().compressorRegistry(compressorRegistry);
	}

	@Test
	void applyWhenHasDecompressorRegistry() {
		DecompressorRegistry decompressorRegistry = mock();
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(null, null, decompressorRegistry,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		then(builder).should().decompressorRegistry(decompressorRegistry);
	}

	@Test
	void applyWhenEmptyCustomizersDoesNothing() {
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		new GrpcChannelBuilderCustomizers(Collections.emptyList()).apply("target", builder);
		then(builder).shouldHaveNoInteractions();
	}

	@Test
	void applyWhenSimpleChannelBuilder() {
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(
				List.of(new SimpleChannelBuilderCustomizer()));
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		then(builder).should().flowControlWindow(100);
	}

	@Test
	@SuppressWarnings("unchecked")
	void applyWhenGenericCustomizersRespectsGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestNettyChannelBuilderCustomizer());
		list.add(new TestShadedNettyChannelBuilderCustomizer());
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(list);
		customizers.apply("target", mock(ManagedChannelBuilder.class));
		assertThat(list.get(0).getCount()).isOne();
		assertThat(list.get(1).getCount()).isZero();
		assertThat(list.get(2).getCount()).isZero();
		customizers.apply("target", mock(NettyChannelBuilder.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isZero();
		customizers.apply("target", mock(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class));
		assertThat(list.get(0).getCount()).isEqualTo(3);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isOne();
	}

	@Test
	void applyWhenHasGrpcClientDefaultServiceConfigCustomizers() {
		GrpcClientDefaultServiceConfigCustomizer defaultConfigCustomizer1 = (target, defaultServiceConfig) -> {
			defaultServiceConfig.put("c", "v1");
			defaultServiceConfig.put("c1", "v1");
		};
		GrpcClientDefaultServiceConfigCustomizer defaultConfigCustomizer2 = (target, defaultServiceConfig) -> {
			defaultServiceConfig.put("c", "v2");
			defaultServiceConfig.put("c2", "v2");
		};
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(null, null, null,
				Collections.emptyList(), List.of(defaultConfigCustomizer1, defaultConfigCustomizer2));
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("c", "v2");
		expected.put("c1", "v1");
		expected.put("c2", "v2");
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void applyWhenHasChannelHealthAddsHealthServiceConfig() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		channel.getHealth().setServiceName("testservice");
		properties.getChannel().put("target", channel);
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", "testservice"));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void applyWhenHasDefaultHealthAddsHealthServiceConfig() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		channel.getHealth().setServiceName("testdefaultservice");
		properties.getChannel().put("default", channel);
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", "testdefaultservice"));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void applyWhenHasServiceConfig() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		ServiceConfig serviceConfig = new ServiceConfig(null, null, null, new HealthCheckConfig("test"));
		channel.setServiceConfig(serviceConfig);
		properties.getChannel().put("default", channel);
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", "test"));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void applyWhenHasClashingServiceConfigAndHealth() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		channel.getHealth().setServiceName("fromhealth");
		ServiceConfig serviceConfig = new ServiceConfig(null, null, null, new HealthCheckConfig("fromservice"));
		channel.setServiceConfig(serviceConfig);
		properties.getChannel().put("default", channel);
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		assertThatIllegalStateException().isThrownBy(() -> customizers.apply("target", builder))
			.withMessage("Unable to change health check config service name from 'fromservice' to 'fromhealth'");
	}

	@Test
	void applyWhenHealthEnabledAndNoServiceNameAddsHealthConfig() {
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		properties.getChannel().put("target", channel);
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", ""));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void applyWhenNoCustomizersOrHealthDoesNotSetDefaultServiceConfig() {
		GrpcClientProperties properties = new GrpcClientProperties();
		GrpcChannelBuilderCustomizers customizers = new GrpcChannelBuilderCustomizers(properties, null, null,
				Collections.emptyList(), Collections.emptyList());
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);
		customizers.apply("target", builder);
		then(builder).should(never()).defaultServiceConfig(any());
	}

	static class SimpleChannelBuilderCustomizer implements GrpcChannelBuilderCustomizer<NettyChannelBuilder> {

		@Override
		public void customize(String target, NettyChannelBuilder channelBuilder) {
			channelBuilder.flowControlWindow(100);
		}

	}

	/**
	 * Test customizer that will match any {@link GrpcChannelBuilderCustomizer}.
	 *
	 * @param <T> the builder type
	 */
	static class TestCustomizer<T extends ManagedChannelBuilder<T>> implements GrpcChannelBuilderCustomizer<T> {

		private int count;

		@Override
		public void customize(String targetOrChannelName, T channelBuilder) {
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
