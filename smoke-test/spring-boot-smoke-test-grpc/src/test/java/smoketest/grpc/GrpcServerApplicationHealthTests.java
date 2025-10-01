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

package smoketest.grpc;

import java.time.Duration;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import io.grpc.protobuf.services.HealthStatusManager;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import smoketest.grpc.proto.HelloReply;
import smoketest.grpc.proto.HelloRequest;
import smoketest.grpc.proto.SimpleGrpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureInProcessTransport;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for gRPC server health feature.
 */
class GrpcServerApplicationHealthTests {

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.port=0",
			"spring.grpc.client.channels.health-test.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.channels.health-test.health.enabled=true",
			"spring.grpc.client.channels.health-test.health.service-name=my-service" })
	@DirtiesContext
	class WithClientHealthEnabled {

		@Test
		void loadBalancerRespectsServerHealth(@Autowired GrpcChannelFactory channels,
				@Autowired HealthStatusManager healthStatusManager) {
			ManagedChannel channel = channels.createChannel("health-test");
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channel);

			// put the service up (SERVING) and give load balancer time to update
			updateHealthStatusAndWait("my-service", ServingStatus.SERVING, healthStatusManager);

			// initially the status should be SERVING
			assertThatResponseIsServedToChannel(client);

			// put the service down (NOT_SERVING) and give load balancer time to update
			updateHealthStatusAndWait("my-service", ServingStatus.NOT_SERVING, healthStatusManager);

			// now the request should fail
			assertThatResponseIsNotServedToChannel(client);

			// put the service up (SERVING) and give load balancer time to update
			updateHealthStatusAndWait("my-service", ServingStatus.SERVING, healthStatusManager);

			// now the request should pass
			assertThatResponseIsServedToChannel(client);
		}

		private void updateHealthStatusAndWait(String serviceName, ServingStatus healthStatus,
				HealthStatusManager healthStatusManager) {
			healthStatusManager.setStatus(serviceName, healthStatus);
			try {
				Thread.sleep(2000L);
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}

		private void assertThatResponseIsServedToChannel(SimpleGrpc.SimpleBlockingStub client) {
			HelloReply response = client.sayHello(HelloRequest.newBuilder().setName("Alien").build());
			assertThat(response.getMessage()).isEqualTo("Hello ==> Alien");
		}

		private void assertThatResponseIsNotServedToChannel(SimpleGrpc.SimpleBlockingStub client) {
			assertThatExceptionOfType(StatusRuntimeException.class)
				.isThrownBy(() -> client.sayHello(HelloRequest.newBuilder().setName("Alien").build()))
				.withMessageContaining("UNAVAILABLE: Health-check service responded NOT_SERVING for 'my-service'");
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.health.actuator.health-indicator-paths=custom",
			"spring.grpc.server.health.actuator.update-initial-delay=3s",
			"spring.grpc.server.health.actuator.update-rate=3s", "management.health.defaults.enabled=true" })
	@AutoConfigureInProcessTransport
	@DirtiesContext
	class WithActuatorHealthAdapter {

		@Test
		void healthIndicatorsAdaptedToGrpcHealthStatus(@Autowired GrpcChannelFactory channels) {
			var channel = channels.createChannel("0.0.0.0:0");
			var healthStub = HealthGrpc.newBlockingStub(channel);
			var serviceName = "custom";

			// initially the status should be SERVING
			assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.SERVING, Duration.ofSeconds(4));

			// put the service down and the status should then be NOT_SERVING
			CustomHealthIndicator.SERVICE_IS_UP = false;
			assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.NOT_SERVING, Duration.ofSeconds(4));

			// put the service up and the status should be SERVING
			CustomHealthIndicator.SERVICE_IS_UP = true;
			assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.SERVING, Duration.ofSeconds(4));
		}

		private void assertThatGrpcHealthStatusIs(HealthBlockingStub healthBlockingStub, String service,
				ServingStatus expectedStatus, Duration maxWaitTime) {
			Awaitility.await().atMost(maxWaitTime).ignoreException(StatusRuntimeException.class).untilAsserted(() -> {
				var healthRequest = HealthCheckRequest.newBuilder().setService(service).build();
				var healthResponse = healthBlockingStub.check(healthRequest);
				assertThat(healthResponse.getStatus()).isEqualTo(expectedStatus);
				// verify the overall status as well
				var overallHealthRequest = HealthCheckRequest.newBuilder().setService("").build();
				var overallHealthResponse = healthBlockingStub.check(overallHealthRequest);
				assertThat(overallHealthResponse.getStatus()).isEqualTo(expectedStatus);
			});
		}

		@TestConfiguration
		static class MyHealthIndicatorsConfig {

			@ConditionalOnEnabledHealthIndicator("custom")
			@Bean
			CustomHealthIndicator customHealthIndicator() {
				return new CustomHealthIndicator();
			}

		}

		static class CustomHealthIndicator implements HealthIndicator {

			static boolean SERVICE_IS_UP = true;

			@Override
			public Health health() {
				return SERVICE_IS_UP ? Health.up().build() : Health.down().build();
			}

		}

	}

}
