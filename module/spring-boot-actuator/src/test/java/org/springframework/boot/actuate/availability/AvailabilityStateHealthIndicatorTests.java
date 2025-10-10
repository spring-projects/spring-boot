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

package org.springframework.boot.actuate.availability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link AvailabilityStateHealthIndicator}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityStateHealthIndicatorTests {

	@Mock
	@SuppressWarnings("NullAway.Init")
	private ApplicationAvailability applicationAvailability;

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenApplicationAvailabilityIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AvailabilityStateHealthIndicator(null, LivenessState.class, (statusMappings) -> {
			}))
			.withMessage("'applicationAvailability' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenStateTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new AvailabilityStateHealthIndicator(this.applicationAvailability, null, (statusMappings) -> {
				}))
			.withMessage("'stateType' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenStatusMappingIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new AvailabilityStateHealthIndicator(this.applicationAvailability, LivenessState.class, null))
			.withMessage("'statusMappings' must not be null");
	}

	@Test
	void createWhenStatusMappingDoesNotCoverAllEnumsThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new AvailabilityStateHealthIndicator(this.applicationAvailability, LivenessState.class,
					(statusMappings) -> statusMappings.add(LivenessState.CORRECT, Status.UP)))
			.withMessage("StatusMappings does not include BROKEN");
	}

	@Test
	void healthReturnsMappedStatus() {
		AvailabilityStateHealthIndicator indicator = new AvailabilityStateHealthIndicator(this.applicationAvailability,
				LivenessState.class, (statusMappings) -> {
					statusMappings.add(LivenessState.CORRECT, Status.UP);
					statusMappings.add(LivenessState.BROKEN, Status.DOWN);
				});
		given(this.applicationAvailability.getState(LivenessState.class)).willReturn(LivenessState.BROKEN);
		Health health = indicator.health(false);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	void healthReturnsDefaultStatus() {
		AvailabilityStateHealthIndicator indicator = new AvailabilityStateHealthIndicator(this.applicationAvailability,
				LivenessState.class, (statusMappings) -> {
					statusMappings.add(LivenessState.CORRECT, Status.UP);
					statusMappings.addDefaultStatus(Status.UNKNOWN);
				});
		given(this.applicationAvailability.getState(LivenessState.class)).willReturn(LivenessState.BROKEN);
		Health health = indicator.health(false);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
	}

	@Test
	void healthWhenNotEnumReturnsMappedStatus() {
		AvailabilityStateHealthIndicator indicator = new AvailabilityStateHealthIndicator(this.applicationAvailability,
				TestAvailabilityState.class, (statusMappings) -> {
					statusMappings.add(TestAvailabilityState.ONE, Status.UP);
					statusMappings.addDefaultStatus(Status.DOWN);
				});
		given(this.applicationAvailability.getState(TestAvailabilityState.class)).willReturn(TestAvailabilityState.TWO);
		Health health = indicator.health(false);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	static class TestAvailabilityState implements AvailabilityState {

		static final TestAvailabilityState ONE = new TestAvailabilityState();

		static final TestAvailabilityState TWO = new TestAvailabilityState();

	}

}
