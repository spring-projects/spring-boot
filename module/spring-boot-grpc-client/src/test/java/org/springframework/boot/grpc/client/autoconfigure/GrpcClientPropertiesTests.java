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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.ChannelConfig;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link GrpcClientProperties}.
 *
 * @author Chris Bono
 */
class GrpcClientPropertiesTests {

	private GrpcClientProperties bindProperties(Map<String, String> map) {
		return new Binder(new MapConfigurationPropertySource(map))
			.bind("spring.grpc.client", GrpcClientProperties.class)
			.get();
	}

	private GrpcClientProperties newProperties(ChannelConfig defaultChannel, Map<String, ChannelConfig> channels) {
		var properties = new GrpcClientProperties();
		ReflectionTestUtils.setField(properties, "defaultChannel", defaultChannel);
		ReflectionTestUtils.setField(properties, "channels", channels);
		return properties;
	}

	@Nested
	class BindPropertiesAPI {

		@Test
		void defaultChannelWithDefaultValues() {
			this.withDefaultValues("default-channel", GrpcClientProperties::getDefaultChannel);
		}

		@Test
		void specificChannelWithDefaultValues() {
			this.withDefaultValues("channels.c1", (p) -> p.getChannel("c1"));
		}

		private void withDefaultValues(String channelName,
				Function<GrpcClientProperties, ChannelConfig> channelFromProperties) {
			Map<String, String> map = new HashMap<>();
			// we have to at least bind one property or bind() fails
			map.put("spring.grpc.client.%s.enable-keep-alive".formatted(channelName), "false");
			GrpcClientProperties properties = bindProperties(map);
			var channel = channelFromProperties.apply(properties);
			assertThat(channel.getAddress()).isEqualTo("static://localhost:9090");
			assertThat(channel.getDefaultLoadBalancingPolicy()).isEqualTo("round_robin");
			assertThat(channel.getHealth().isEnabled()).isFalse();
			assertThat(channel.getHealth().getServiceName()).isNull();
			assertThat(channel.getNegotiationType()).isEqualTo(NegotiationType.PLAINTEXT);
			assertThat(channel.isEnableKeepAlive()).isFalse();
			assertThat(channel.getIdleTimeout()).isEqualTo(Duration.ofSeconds(20));
			assertThat(channel.getKeepAliveTime()).isEqualTo(Duration.ofMinutes(5));
			assertThat(channel.getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(20));
			assertThat(channel.isEnableKeepAlive()).isFalse();
			assertThat(channel.isKeepAliveWithoutCalls()).isFalse();
			assertThat(channel.getMaxInboundMessageSize()).isEqualTo(DataSize.ofBytes(4194304));
			assertThat(channel.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofBytes(8192));
			assertThat(channel.getUserAgent()).isNull();
			assertThat(channel.isSecure()).isTrue();
			assertThat(channel.getSsl().isEnabled()).isNull();
			assertThat(channel.getSsl().determineEnabled()).isFalse();
			assertThat(channel.getSsl().getBundle()).isNull();
		}

		@Test
		void defaultChannelWithSpecifiedValues() {
			this.withSpecifiedValues("default-channel", GrpcClientProperties::getDefaultChannel);
		}

		@Test
		void specificChannelWithSpecifiedValues() {
			this.withSpecifiedValues("channels.c1", (p) -> p.getChannel("c1"));
		}

		private void withSpecifiedValues(String channelName,
				Function<GrpcClientProperties, ChannelConfig> channelFromProperties) {
			Map<String, String> map = new HashMap<>();
			var propPrefix = "spring.grpc.client.%s.".formatted(channelName);
			map.put("%s.address".formatted(propPrefix), "static://my-server:8888");
			map.put("%s.default-load-balancing-policy".formatted(propPrefix), "pick_first");
			map.put("%s.health.enabled".formatted(propPrefix), "true");
			map.put("%s.health.service-name".formatted(propPrefix), "my-service");
			map.put("%s.negotiation-type".formatted(propPrefix), "plaintext_upgrade");
			map.put("%s.enable-keep-alive".formatted(propPrefix), "true");
			map.put("%s.idle-timeout".formatted(propPrefix), "1m");
			map.put("%s.keep-alive-time".formatted(propPrefix), "200s");
			map.put("%s.keep-alive-timeout".formatted(propPrefix), "60000ms");
			map.put("%s.keep-alive-without-calls".formatted(propPrefix), "true");
			map.put("%s.max-inbound-message-size".formatted(propPrefix), "200MB");
			map.put("%s.max-inbound-metadata-size".formatted(propPrefix), "1GB");
			map.put("%s.user-agent".formatted(propPrefix), "me");
			map.put("%s.secure".formatted(propPrefix), "false");
			map.put("%s.ssl.enabled".formatted(propPrefix), "true");
			map.put("%s.ssl.bundle".formatted(propPrefix), "my-bundle");
			GrpcClientProperties properties = bindProperties(map);
			var channel = channelFromProperties.apply(properties);
			assertThat(channel.getAddress()).isEqualTo("static://my-server:8888");
			assertThat(channel.getDefaultLoadBalancingPolicy()).isEqualTo("pick_first");
			assertThat(channel.getHealth().isEnabled()).isTrue();
			assertThat(channel.getHealth().getServiceName()).isEqualTo("my-service");
			assertThat(channel.getNegotiationType()).isEqualTo(NegotiationType.PLAINTEXT_UPGRADE);
			assertThat(channel.isEnableKeepAlive()).isTrue();
			assertThat(channel.getIdleTimeout()).isEqualTo(Duration.ofMinutes(1));
			assertThat(channel.getKeepAliveTime()).isEqualTo(Duration.ofSeconds(200));
			assertThat(channel.getKeepAliveTimeout()).isEqualTo(Duration.ofMillis(60000));
			assertThat(channel.isEnableKeepAlive()).isTrue();
			assertThat(channel.isKeepAliveWithoutCalls()).isTrue();
			assertThat(channel.getMaxInboundMessageSize()).isEqualTo(DataSize.ofMegabytes(200));
			assertThat(channel.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofGigabytes(1));
			assertThat(channel.getUserAgent()).isEqualTo("me");
			assertThat(channel.isSecure()).isFalse();
			assertThat(channel.getSsl().isEnabled()).isTrue();
			assertThat(channel.getSsl().determineEnabled()).isTrue();
			assertThat(channel.getSsl().getBundle()).isEqualTo("my-bundle");
		}

		@Test
		void withoutKeepAliveUnitsSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.default-channel.idle-timeout", "1");
			map.put("spring.grpc.client.default-channel.keep-alive-time", "60");
			map.put("spring.grpc.client.default-channel.keep-alive-timeout", "5");
			GrpcClientProperties properties = bindProperties(map);
			var defaultChannel = properties.getDefaultChannel();
			assertThat(defaultChannel.getIdleTimeout()).isEqualTo(Duration.ofSeconds(1));
			assertThat(defaultChannel.getKeepAliveTime()).isEqualTo(Duration.ofSeconds(60));
			assertThat(defaultChannel.getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(5));
		}

		@Test
		void withoutInboundSizeUnitsSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.default-channel.max-inbound-message-size", "1000");
			map.put("spring.grpc.client.default-channel.max-inbound-metadata-size", "256");
			GrpcClientProperties properties = bindProperties(map);
			var defaultChannel = properties.getDefaultChannel();
			assertThat(defaultChannel.getMaxInboundMessageSize()).isEqualTo(DataSize.ofBytes(1000));
			assertThat(defaultChannel.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofBytes(256));
		}

		@Test
		void withServiceConfig() {
			Map<String, String> map = new HashMap<>();
			// we have to at least bind one property or bind() fails
			map.put("spring.grpc.client.%s.service-config.something.key".formatted("default-channel"), "value");
			GrpcClientProperties properties = bindProperties(map);
			var channel = properties.getDefaultChannel();
			assertThat(channel.getServiceConfig()).hasSize(1);
			assertThat(channel.getServiceConfig().get("something")).isInstanceOf(Map.class);
		}

		@Test
		void whenBundleNameSetThenDetermineEnabledReturnsTrue() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.client.default-channel.ssl.bundle", "my-bundle");
			GrpcClientProperties properties = bindProperties(map);
			var channel = properties.getDefaultChannel();
			assertThat(channel.getSsl().isEnabled()).isNull();
			assertThat(channel.getSsl().determineEnabled()).isTrue();
		}

	}

	@Nested
	class GetChannelAPI {

		@Test
		void withDefaultNameReturnsDefaultChannel() {
			var properties = new GrpcClientProperties();
			var defaultChannel = properties.getChannel("default");
			assertThat(properties).extracting("defaultChannel").isSameAs(defaultChannel);
			assertThat(properties).extracting("channels", InstanceOfAssertFactories.MAP).isEmpty();
		}

		@Test
		void withKnownNameReturnsKnownChannel() {
			Map<String, String> map = new HashMap<>();
			// we have to at least bind one property or bind() fails
			map.put("spring.grpc.client.channels.c1.enable-keep-alive", "false");
			GrpcClientProperties properties = bindProperties(map);
			var channel = properties.getChannel("c1");
			assertThat(properties).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel));
		}

		@Test
		void withUnknownNameReturnsNewChannelWithCopiedDefaults() {
			var defaultChannel = new ChannelConfig();
			defaultChannel.setAddress("static://my-server:9999");
			defaultChannel.setDefaultLoadBalancingPolicy("custom");
			defaultChannel.getHealth().setEnabled(true);
			defaultChannel.getHealth().setServiceName("custom-service");
			defaultChannel.setEnableKeepAlive(true);
			defaultChannel.setIdleTimeout(Duration.ofMinutes(1));
			defaultChannel.setKeepAliveTime(Duration.ofMinutes(4));
			defaultChannel.setKeepAliveTimeout(Duration.ofMinutes(6));
			defaultChannel.setKeepAliveWithoutCalls(true);
			defaultChannel.setMaxInboundMessageSize(DataSize.ofMegabytes(100));
			defaultChannel.setMaxInboundMetadataSize(DataSize.ofMegabytes(200));
			defaultChannel.setUserAgent("me");
			defaultChannel.setDefaultDeadline(Duration.ofMinutes(1));
			defaultChannel.getSsl().setEnabled(true);
			defaultChannel.getSsl().setBundle("custom-bundle");
			var properties = newProperties(defaultChannel, Collections.emptyMap());
			var newChannel = properties.getChannel("new-channel");
			assertThat(newChannel).usingRecursiveComparison().isEqualTo(defaultChannel);
			assertThat(properties).extracting("channels", InstanceOfAssertFactories.MAP).isEmpty();
		}

		@Test
		void withUnknownNameReturnsNewChannelWithOwnAddress() {
			var defaultChannel = new ChannelConfig();
			defaultChannel.setAddress("static://my-server:9999");
			var properties = newProperties(defaultChannel, Collections.emptyMap());
			var newChannel = properties.getChannel("other-server:8888");
			assertThat(newChannel).usingRecursiveComparison().ignoringFields("address").isEqualTo(defaultChannel);
			assertThat(newChannel).hasFieldOrPropertyWithValue("address", "static://other-server:8888");
			assertThat(properties).extracting("channels", InstanceOfAssertFactories.MAP).isEmpty();
		}

	}

	@Nested
	class GetTargetAPI {

		@Test
		void channelWithStaticAddressReturnsStrippedAddress() {
			var defaultChannel = new ChannelConfig();
			var channel1 = new ChannelConfig();
			channel1.setAddress("static://my-server:8888");
			var properties = newProperties(defaultChannel, Map.of("c1", channel1));
			assertThat(properties.getTarget("c1")).isEqualTo("my-server:8888");
			assertThat(properties).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel1));
		}

		@Test
		void channelWithTcpAddressReturnsStrippedAddress() {
			var defaultChannel = new ChannelConfig();
			var channel1 = new ChannelConfig();
			channel1.setAddress("tcp://my-server:8888");
			var properties = newProperties(defaultChannel, Map.of("c1", channel1));
			assertThat(properties.getTarget("c1")).isEqualTo("my-server:8888");
			assertThat(properties).extracting("channels", InstanceOfAssertFactories.MAP)
				.containsExactly(entry("c1", channel1));
		}

		@Test
		void channelWithAddressPropertyPlaceholdersPopulatesFromEnvironment() {
			var defaultChannel = new ChannelConfig();
			var channel1 = new ChannelConfig();
			channel1.setAddress("my-server-${channelName}:8888");
			var properties = newProperties(defaultChannel, Map.of("c1", channel1));
			var env = new MockEnvironment();
			env.setProperty("channelName", "foo");
			properties.setEnvironment(env);
			assertThat(properties.getTarget("c1")).isEqualTo("my-server-foo:8888");
		}

	}

	@Nested
	class CopyDefaultsAPI {

		@Test
		void copyFromDefaultChannel() {
			var properties = new GrpcClientProperties();
			var defaultChannel = properties.getDefaultChannel();
			var newChannel = defaultChannel.copy();
			assertThat(newChannel).usingRecursiveComparison().isEqualTo(defaultChannel);
			assertThat(newChannel.getServiceConfig()).isEqualTo(defaultChannel.getServiceConfig());
		}

	}

}
