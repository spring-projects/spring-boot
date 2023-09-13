/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceReadinessChecks}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceReadinessChecksTests {

	private Clock clock;

	Instant now = Instant.now();

	private RunningService runningService;

	private List<RunningService> runningServices;

	@BeforeEach
	void setup() {
		this.clock = mock(Clock.class);
		given(this.clock.instant()).willAnswer((args) -> this.now);
		this.runningService = mock(RunningService.class);
		this.runningServices = List.of(this.runningService);
	}

	@Test
	void waitUntilReadyWhenImmediatelyReady() {
		MockServiceReadinessCheck check = new MockServiceReadinessCheck();
		createChecks(check).waitUntilReady(this.runningServices);
		assertThat(check.getChecked()).contains(this.runningService);
	}

	@Test
	void waitUntilReadyWhenTakesTimeToBeReady() {
		MockServiceReadinessCheck check = new MockServiceReadinessCheck(2);
		createChecks(check).waitUntilReady(this.runningServices);
		assertThat(check.getChecked()).hasSize(2).contains(this.runningService);
	}

	@Test
	void waitUntilReadyWhenTimeout() {
		MockServiceReadinessCheck check = new MockServiceReadinessCheck(Integer.MAX_VALUE);
		assertThatExceptionOfType(ReadinessTimeoutException.class)
			.isThrownBy(() -> createChecks(check).waitUntilReady(this.runningServices))
			.satisfies((ex) -> assertThat(ex.getSuppressed()).hasSize(1));
		assertThat(check.getChecked()).hasSizeGreaterThan(10);
	}

	@Test
	void waitForWhenServiceHasDisableLabelDoesNotCheck() {
		given(this.runningService.labels()).willReturn(Map.of("org.springframework.boot.readiness-check.disable", ""));
		MockServiceReadinessCheck check = new MockServiceReadinessCheck();
		createChecks(check).waitUntilReady(this.runningServices);
		assertThat(check.getChecked()).isEmpty();
	}

	void sleep(Duration duration) {
		this.now = this.now.plus(duration);
	}

	private ServiceReadinessChecks createChecks(TcpConnectServiceReadinessCheck check) {
		DockerComposeProperties properties = new DockerComposeProperties();
		return new ServiceReadinessChecks(properties.getReadiness(), this.clock, this::sleep, check);
	}

	/**
	 * Mock {@link TcpConnectServiceReadinessCheck}.
	 */
	static class MockServiceReadinessCheck extends TcpConnectServiceReadinessCheck {

		private final Integer failUntil;

		private final List<RunningService> checked = new ArrayList<>();

		MockServiceReadinessCheck() {
			this(null);
		}

		MockServiceReadinessCheck(Integer failUntil) {
			super(null);
			this.failUntil = failUntil;
		}

		@Override
		public void check(RunningService service) throws ServiceNotReadyException {
			this.checked.add(service);
			if (this.failUntil != null && this.checked.size() < this.failUntil) {
				throw new ServiceNotReadyException(service, "Waiting");
			}
		}

		List<RunningService> getChecked() {
			return this.checked;
		}

	}

}
