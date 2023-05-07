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

package org.springframework.boot.docker.compose.readiness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

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

	private MockSpringFactoriesLoader loader;

	private ClassLoader classLoader;

	private MockEnvironment environment;

	private Binder binder;

	private RunningService runningService;

	private List<RunningService> runningServices;

	private final MockServiceReadinessCheck mockTcpCheck = new MockServiceReadinessCheck();

	@BeforeEach
	void setup() {
		this.clock = mock(Clock.class);
		given(this.clock.instant()).willAnswer((args) -> this.now);
		this.loader = new MockSpringFactoriesLoader();
		this.classLoader = getClass().getClassLoader();
		this.environment = new MockEnvironment();
		this.binder = Binder.get(this.environment);
		this.runningService = mock(RunningService.class);
		this.runningServices = List.of(this.runningService);
	}

	@Test
	void loadCanResolveArguments() {
		this.loader = spy(MockSpringFactoriesLoader.class);
		createChecks();
		then(this.loader).should()
			.load(eq(ServiceReadinessCheck.class), ArgumentMatchers.<ArgumentResolver>assertArg((argumentResolver) -> {
				assertThat(argumentResolver.resolve(ClassLoader.class)).isEqualTo(this.classLoader);
				assertThat(argumentResolver.resolve(Environment.class)).isEqualTo(this.environment);
				assertThat(argumentResolver.resolve(Binder.class)).isEqualTo(this.binder);
			}));
	}

	@Test
	void waitUntilReadyWhenImmediatelyReady() {
		MockServiceReadinessCheck check = new MockServiceReadinessCheck();
		this.loader.addInstance(ServiceReadinessCheck.class, check);
		createChecks().waitUntilReady(this.runningServices);
		assertThat(check.getChecked()).contains(this.runningService);
		assertThat(this.mockTcpCheck.getChecked()).contains(this.runningService);
	}

	@Test
	void waitUntilReadyWhenTakesTimeToBeReady() {
		MockServiceReadinessCheck check = new MockServiceReadinessCheck(2);
		this.loader.addInstance(ServiceReadinessCheck.class, check);
		createChecks().waitUntilReady(this.runningServices);
		assertThat(check.getChecked()).hasSize(2).contains(this.runningService);
		assertThat(this.mockTcpCheck.getChecked()).contains(this.runningService);
	}

	@Test
	void waitUntilReadyWhenTimeout() {
		MockServiceReadinessCheck check = new MockServiceReadinessCheck(Integer.MAX_VALUE);
		this.loader.addInstance(ServiceReadinessCheck.class, check);
		assertThatExceptionOfType(ReadinessTimeoutException.class)
			.isThrownBy(() -> createChecks().waitUntilReady(this.runningServices))
			.satisfies((ex) -> assertThat(ex.getSuppressed()).hasSize(1));
		assertThat(check.getChecked()).hasSizeGreaterThan(10);
	}

	@Test
	void waitForWhenServiceHasDisableLabelDoesNotCheck() {
		given(this.runningService.labels()).willReturn(Map.of("org.springframework.boot.readiness-check.disable", ""));
		MockServiceReadinessCheck check = new MockServiceReadinessCheck();
		this.loader.addInstance(ServiceReadinessCheck.class, check);
		createChecks().waitUntilReady(this.runningServices);
		assertThat(check.getChecked()).isEmpty();
		assertThat(this.mockTcpCheck.getChecked()).isEmpty();
	}

	void sleep(Duration duration) {
		this.now = this.now.plus(duration);
	}

	private ServiceReadinessChecks createChecks() {
		return new ServiceReadinessChecks(this.clock, this::sleep, this.loader, this.classLoader, this.environment,
				this.binder, (properties) -> this.mockTcpCheck);
	}

	/**
	 * Mock {@link ServiceReadinessCheck}.
	 */
	static class MockServiceReadinessCheck implements ServiceReadinessCheck {

		private final Integer failUntil;

		private final List<RunningService> checked = new ArrayList<>();

		MockServiceReadinessCheck() {
			this(null);
		}

		MockServiceReadinessCheck(Integer failUntil) {
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
