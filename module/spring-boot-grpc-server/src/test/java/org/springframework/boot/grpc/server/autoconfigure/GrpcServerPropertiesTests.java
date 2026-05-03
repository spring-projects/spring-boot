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

import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.grpc.TlsServerCredentials.ClientAuth;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GrpcServerProperties}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcServerPropertiesTests {

	private GrpcServerProperties bindProperties(Map<String, String> map) {
		return new Binder(new MapConfigurationPropertySource(map))
			.bind("spring.grpc.server", GrpcServerProperties.class)
			.get();
	}

	@Test
	void bind() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("spring.grpc.server.address", "192.168.0.1");
		GrpcServerProperties properties = bindProperties(map);
		assertThat(properties.getAddress()).isEqualTo(InetAddress.getByName("192.168.0.1"));
	}

	@Test
	void defaultAddressIsNull() {
		assertThat(new GrpcServerProperties().getAddress()).isNull();
	}

	@Nested
	class ShutdownProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.shutdown.grace-period", "10m");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getShutdown().getGracePeriod()).isEqualTo(Duration.ofMinutes(10));
		}

		@Test
		void bindWithoutUnits() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.shutdown.grace-period", "10");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getShutdown().getGracePeriod()).isEqualTo(Duration.ofSeconds(10));
		}

	}

	@Nested
	class InboundProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.inbound.message.max-size", "20MB");
			map.put("spring.grpc.server.inbound.metadata.max-size", "1MB");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getInbound().getMessage().getMaxSize()).isEqualTo(DataSize.ofMegabytes(20));
			assertThat(properties.getInbound().getMetadata().getMaxSize()).isEqualTo(DataSize.ofMegabytes(1));
		}

		@Test
		void bindWithoutUnits() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.inbound.message.max-size", "1048576");
			map.put("spring.grpc.server.inbound.metadata.max-size", "1024");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getInbound().getMessage().getMaxSize()).isEqualTo(DataSize.ofMegabytes(1));
			assertThat(properties.getInbound().getMetadata().getMaxSize()).isEqualTo(DataSize.ofKilobytes(1));
		}

	}

	@Nested
	class KeepAliveProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.keepalive.time", "45m");
			map.put("spring.grpc.server.keepalive.timeout", "40s");
			map.put("spring.grpc.server.keepalive.permit.time", "33s");
			map.put("spring.grpc.server.keepalive.permit.without-calls", "true");
			map.put("spring.grpc.server.keepalive.connection.max-idle-time", "1h");
			map.put("spring.grpc.server.keepalive.connection.max-age", "3h");
			map.put("spring.grpc.server.keepalive.connection.grace-period", "21s");
			GrpcServerProperties.Keepalive properties = bindProperties(map).getKeepalive();
			assertThatPropertiesSetAsExpected(properties);
		}

		@Test
		void bindWithoutUnits() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.keepalive.time", "2700");
			map.put("spring.grpc.server.keepalive.timeout", "40");
			map.put("spring.grpc.server.keepalive.permit.time", "33");
			map.put("spring.grpc.server.keepalive.permit.without-calls", "true");
			map.put("spring.grpc.server.keepalive.connection.max-idle-time", "3600");
			map.put("spring.grpc.server.keepalive.connection.max-age", "10800");
			map.put("spring.grpc.server.keepalive.connection.grace-period", "21");
			GrpcServerProperties.Keepalive properties = bindProperties(map).getKeepalive();
			assertThatPropertiesSetAsExpected(properties);
		}

		private void assertThatPropertiesSetAsExpected(GrpcServerProperties.Keepalive properties) {
			assertThat(properties.getTime()).isEqualTo(Duration.ofMinutes(45));
			assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(40));
			assertThat(properties.getPermit().getTime()).isEqualTo(Duration.ofSeconds(33));
			assertThat(properties.getPermit().isWithoutCalls()).isTrue();
			assertThat(properties.getConnection().getMaxIdleTime()).isEqualTo(Duration.ofHours(1));
			assertThat(properties.getConnection().getMaxAge()).isEqualTo(Duration.ofHours(3));
			assertThat(properties.getConnection().getGracePeriod()).isEqualTo(Duration.ofSeconds(21));
		}

	}

	@Nested
	class SslProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.ssl.enabled", "true");
			map.put("spring.grpc.server.ssl.client-auth", "require");
			map.put("spring.grpc.server.ssl.bundle", "test");
			map.put("spring.grpc.server.ssl.secure", "false");
			GrpcServerProperties.Ssl properties = bindProperties(map).getSsl();
			assertThat(properties.getEnabled()).isTrue();
			assertThat(properties.getClientAuth()).isEqualTo(ClientAuth.REQUIRE);
			assertThat(properties.getBundle()).isEqualTo("test");
			assertThat(properties.isSecure()).isFalse();
		}

	}

}
