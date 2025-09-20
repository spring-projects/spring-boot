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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link GrpcServerProperties}.
 *
 * @author Chris Bono
 */
class GrpcServerPropertiesTests {

	private GrpcServerProperties bindProperties(Map<String, String> map) {
		return new Binder(new MapConfigurationPropertySource(map))
			.bind("spring.grpc.server", GrpcServerProperties.class)
			.get();
	}

	@Nested
	class BaseProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.host", "my-server-ip");
			map.put("spring.grpc.server.port", "3130");
			map.put("spring.grpc.server.shutdown-grace-period", "15");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getAddress()).isEqualTo("my-server-ip:3130");
			assertThat(properties.getPort()).isEqualTo(3130);
			assertThat(properties.getShutdownGracePeriod()).isEqualTo(Duration.ofSeconds(15));
		}

	}

	@Nested
	class HealthProperties {

		@Test
		void bindWithNoSettings() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.host", "my-server-ip");
			GrpcServerProperties.Health properties = bindProperties(map).getHealth();
			assertThat(properties.getEnabled()).isTrue();
			assertThat(properties.getActuator().getEnabled()).isTrue();
			assertThat(properties.getActuator().getHealthIndicatorPaths()).isEmpty();
			assertThat(properties.getActuator().getUpdateOverallHealth()).isTrue();
			assertThat(properties.getActuator().getUpdateRate()).isEqualTo(Duration.ofSeconds(5));
			assertThat(properties.getActuator().getUpdateInitialDelay()).isEqualTo(Duration.ofSeconds(5));
		}

		@Test
		void bindWithoutUnitsSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.health.enabled", "false");
			map.put("spring.grpc.server.health.actuator.enabled", "false");
			map.put("spring.grpc.server.health.actuator.health-indicator-paths", "a,b,c");
			map.put("spring.grpc.server.health.actuator.update-overall-health", "false");
			map.put("spring.grpc.server.health.actuator.update-rate", "2s");
			map.put("spring.grpc.server.health.actuator.update-initial-delay", "1m");
			GrpcServerProperties.Health properties = bindProperties(map).getHealth();
			assertThat(properties.getEnabled()).isFalse();
			assertThat(properties.getActuator().getEnabled()).isFalse();
			assertThat(properties.getActuator().getHealthIndicatorPaths()).containsExactly("a", "b", "c");
			assertThat(properties.getActuator().getUpdateOverallHealth()).isFalse();
			assertThat(properties.getActuator().getUpdateRate()).isEqualTo(Duration.ofSeconds(2));
			assertThat(properties.getActuator().getUpdateInitialDelay()).isEqualTo(Duration.ofMinutes(1));
		}

	}

	@Nested
	class KeepAliveProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.keep-alive.time", "45m");
			map.put("spring.grpc.server.keep-alive.timeout", "40s");
			map.put("spring.grpc.server.keep-alive.max-idle", "1h");
			map.put("spring.grpc.server.keep-alive.max-age", "3h");
			map.put("spring.grpc.server.keep-alive.max-age-grace", "21s");
			map.put("spring.grpc.server.keep-alive.permit-time", "33s");
			map.put("spring.grpc.server.keep-alive.permit-without-calls", "true");
			GrpcServerProperties.KeepAlive properties = bindProperties(map).getKeepAlive();
			assertThatPropertiesSetAsExpected(properties);
		}

		@Test
		void bindWithoutUnitsSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.keep-alive.time", "2700");
			map.put("spring.grpc.server.keep-alive.timeout", "40");
			map.put("spring.grpc.server.keep-alive.max-idle", "3600");
			map.put("spring.grpc.server.keep-alive.max-age", "10800");
			map.put("spring.grpc.server.keep-alive.max-age-grace", "21");
			map.put("spring.grpc.server.keep-alive.permit-time", "33");
			map.put("spring.grpc.server.keep-alive.permit-without-calls", "true");
			GrpcServerProperties.KeepAlive properties = bindProperties(map).getKeepAlive();
			assertThatPropertiesSetAsExpected(properties);
		}

		private void assertThatPropertiesSetAsExpected(GrpcServerProperties.KeepAlive properties) {
			assertThat(properties.getTime()).isEqualTo(Duration.ofMinutes(45));
			assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(40));
			assertThat(properties.getMaxIdle()).isEqualTo(Duration.ofHours(1));
			assertThat(properties.getMaxAge()).isEqualTo(Duration.ofHours(3));
			assertThat(properties.getMaxAgeGrace()).isEqualTo(Duration.ofSeconds(21));
			assertThat(properties.getPermitTime()).isEqualTo(Duration.ofSeconds(33));
			assertThat(properties.isPermitWithoutCalls()).isTrue();
		}

	}

	@Nested
	class InboundLimitsProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.max-inbound-message-size", "20MB");
			map.put("spring.grpc.server.max-inbound-metadata-size", "1MB");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getMaxInboundMessageSize()).isEqualTo(DataSize.ofMegabytes(20));
			assertThat(properties.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofMegabytes(1));
		}

		@Test
		void bindWithoutUnits() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.max-inbound-message-size", "1048576");
			map.put("spring.grpc.server.max-inbound-metadata-size", "1024");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getMaxInboundMessageSize()).isEqualTo(DataSize.ofMegabytes(1));
			assertThat(properties.getMaxInboundMetadataSize()).isEqualTo(DataSize.ofKilobytes(1));
		}

	}

	@Nested
	class AddressProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.address", "my-server-ip:3130");
			GrpcServerProperties properties = bindProperties(map);
			assertThat(properties.getAddress()).isEqualTo("my-server-ip:3130");
			assertThat(properties.getPort()).isEqualTo(3130);
		}

		@Test
		void illegalBecauseAddressAndPortSpecified() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.grpc.server.address", "my-server-ip:3130");
			map.put("spring.grpc.server.port", "10000");
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindProperties(map));
		}

	}

}
