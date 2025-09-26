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

package org.springframework.boot.grpc.server.autoconfigure.health;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.health.HealthDescriptor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ActuatorHealthAdapter}.
 */
class ActuatorHealthAdapterTests {

	private HealthStatusManager mockHealthStatusManager;

	private HealthEndpoint mockHealthEndpoint;

	private StatusAggregator mockStatusAggregator;

	@BeforeEach
	void prepareMocks() {
		this.mockHealthStatusManager = mock();
		this.mockHealthEndpoint = mock();
		this.mockStatusAggregator = mock();
	}

	@Disabled("TODO figure out how to mock HealthDescriptor")
	@Test
	void whenIndicatorPathsFoundStatusIsUpdated() {
		var service1 = "check1";
		var service2 = "component2/check2";
		var service3 = "component3a/component3b/check3";
		given(this.mockHealthEndpoint.healthForPath("check1")).willReturn(healthOf(Status.UP));
		given(this.mockHealthEndpoint.healthForPath("component2", "check2")).willReturn(healthOf(Status.DOWN));
		given(this.mockHealthEndpoint.healthForPath("component3a", "component3b", "check3"))
			.willReturn(healthOf(Status.UNKNOWN));
		given(this.mockStatusAggregator.getAggregateStatus(anySet())).willReturn(Status.UNKNOWN);
		var healthAdapter = new ActuatorHealthAdapter(this.mockHealthStatusManager, this.mockHealthEndpoint,
				this.mockStatusAggregator, true, List.of(service1, service2, service3));
		healthAdapter.updateHealthStatus();
		then(this.mockHealthStatusManager).should().setStatus(service1, ServingStatus.SERVING);
		then(this.mockHealthStatusManager).should().setStatus(service2, ServingStatus.NOT_SERVING);
		then(this.mockHealthStatusManager).should().setStatus(service3, ServingStatus.UNKNOWN);
		ArgumentCaptor<Set<Status>> statusesArgCaptor = ArgumentCaptor.captor();
		then(this.mockStatusAggregator).should().getAggregateStatus(statusesArgCaptor.capture());
		assertThat(statusesArgCaptor.getValue())
			.containsExactlyInAnyOrderElementsOf(Set.of(Status.UP, Status.DOWN, Status.UNKNOWN));
		then(this.mockHealthStatusManager).should().setStatus("", ServingStatus.UNKNOWN);
	}

	@Disabled("TODO figure out how to mock HealthDescriptor")
	@Test
	void whenOverallHealthIsFalseOverallStatusIsNotUpdated() {
		var service1 = "check1";
		given(this.mockHealthEndpoint.healthForPath("check1")).willReturn(healthOf(Status.UP));
		var healthAdapter = new ActuatorHealthAdapter(this.mockHealthStatusManager, this.mockHealthEndpoint,
				this.mockStatusAggregator, false, List.of(service1));
		healthAdapter.updateHealthStatus();
		then(this.mockStatusAggregator).shouldHaveNoInteractions();
		then(this.mockHealthStatusManager).should(never()).setStatus(eq(""), any(ServingStatus.class));
	}

	@Disabled("TODO figure out how to mock HealthDescriptor")
	@Test
	void whenIndicatorPathNotFoundStatusIsNotUpdated() {
		var healthAdapter = new ActuatorHealthAdapter(this.mockHealthStatusManager, this.mockHealthEndpoint,
				this.mockStatusAggregator, false, List.of("check1"));
		healthAdapter.updateHealthStatus();
		then(this.mockHealthStatusManager).shouldHaveNoInteractions();
	}

	@Disabled("TODO figure out how to mock HealthDescriptor")
	@Test
	void whenNoIndicatorPathsSpecifiedThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ActuatorHealthAdapter(this.mockHealthStatusManager, this.mockHealthEndpoint,
					this.mockStatusAggregator, false, Collections.emptyList()))
			.withMessage("at least one health indicator path is required");
	}

	private HealthDescriptor healthOf(Status status) {
		HealthDescriptor healthDescriptor = mock();
		given(healthDescriptor.getStatus()).willReturn(status);
		return healthDescriptor;
	}

	@Nested
	class ToServingStatusApi {

		private final ActuatorHealthAdapter healthAdapter = new ActuatorHealthAdapter(
				ActuatorHealthAdapterTests.this.mockHealthStatusManager,
				ActuatorHealthAdapterTests.this.mockHealthEndpoint,
				ActuatorHealthAdapterTests.this.mockStatusAggregator, false, List.of("check1"));

		@Test
		void whenActuatorStatusIsUpThenServingStatusIsUp() {
			assertThat(this.healthAdapter.toServingStatus(Status.UP.getCode())).isEqualTo(ServingStatus.SERVING);
		}

		@Test
		void whenActuatorStatusIsUnknownThenServingStatusIsUnknown() {
			assertThat(this.healthAdapter.toServingStatus(Status.UNKNOWN.getCode())).isEqualTo(ServingStatus.UNKNOWN);
		}

		@Test
		void whenActuatorStatusIsDownThenServingStatusIsNotServing() {
			assertThat(this.healthAdapter.toServingStatus(Status.DOWN.getCode())).isEqualTo(ServingStatus.NOT_SERVING);
		}

		@Test
		void whenActuatorStatusIsOutOfServiceThenServingStatusIsNotServing() {
			assertThat(this.healthAdapter.toServingStatus(Status.OUT_OF_SERVICE.getCode()))
				.isEqualTo(ServingStatus.NOT_SERVING);
		}

	}

}
