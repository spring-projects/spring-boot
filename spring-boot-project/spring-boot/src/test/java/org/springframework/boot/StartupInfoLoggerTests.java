/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication.Startup;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartupInfoLogger}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
class StartupInfoLoggerTests {

	private final Log log = mock(Log.class);

	private MockEnvironment environment;

	@BeforeEach
	void setUp() {
		this.environment = new MockEnvironment();
		this.environment.setProperty("spring.application.version", "1.2.3");
		this.environment.setProperty("spring.application.pid", "42");
	}

	@Test
	void startingFormat() {
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass(), this.environment).logStarting(this.log);
		then(this.log).should()
			.info(assertArg(
					(message) -> assertThat(message.toString()).contains("Starting " + getClass().getSimpleName()
							+ " v1.2.3 using Java " + System.getProperty("java.version") + " with PID 42 (started by "
							+ System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));
	}

	@Test
	void startingFormatWhenVersionIsNotAvailable() {
		this.environment.setProperty("spring.application.version", "");
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass(), this.environment).logStarting(this.log);
		then(this.log).should()
			.info(assertArg(
					(message) -> assertThat(message.toString()).contains("Starting " + getClass().getSimpleName()
							+ " using Java " + System.getProperty("java.version") + " with PID 42 (started by "
							+ System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));
	}

	@Test
	void startingFormatWhenPidIsNotAvailable() {
		this.environment.setProperty("spring.application.pid", "");
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass(), this.environment).logStarting(this.log);
		then(this.log).should()
			.info(assertArg(
					(message) -> assertThat(message.toString()).contains("Starting " + getClass().getSimpleName()
							+ " v1.2.3 using Java " + System.getProperty("java.version") + " (started by "
							+ System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));
	}

	@Test
	void startingFormatInAotMode() {
		System.setProperty("spring.aot.enabled", "true");
		try {
			given(this.log.isInfoEnabled()).willReturn(true);
			new StartupInfoLogger(getClass(), this.environment).logStarting(this.log);
			then(this.log).should()
				.info(assertArg((message) -> assertThat(message.toString())
					.contains("Starting AOT-processed " + getClass().getSimpleName() + " v1.2.3 using Java "
							+ System.getProperty("java.version") + " with PID 42 (started by "
							+ System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));

		}
		finally {
			System.clearProperty("spring.aot.enabled");
		}
	}

	@Test
	void startedFormat() {
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass(), this.environment).logStarted(this.log, new TestStartup(1345L, "Started"));
		then(this.log).should()
			.info(assertArg((message) -> assertThat(message.toString()).matches("Started " + getClass().getSimpleName()
					+ " in \\d+\\.\\d{1,3} seconds \\(process running for 1.345\\)")));
	}

	@Test
	void startedWithoutUptimeFormat() {
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass(), this.environment).logStarted(this.log, new TestStartup(null, "Started"));
		then(this.log).should()
			.info(assertArg((message) -> assertThat(message.toString())
				.matches("Started " + getClass().getSimpleName() + " in \\d+\\.\\d{1,3} seconds")));
	}

	@Test
	void restoredFormat() {
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass(), this.environment).logStarted(this.log, new TestStartup(null, "Restored"));
		then(this.log).should()
			.info(assertArg((message) -> assertThat(message.toString())
				.matches("Restored " + getClass().getSimpleName() + " in \\d+\\.\\d{1,3} seconds")));
	}

	static class TestStartup extends Startup {

		private final long startTime = System.currentTimeMillis();

		private final Long uptime;

		private final String action;

		TestStartup(Long uptime, String action) {
			this.uptime = uptime;
			this.action = action;
			started();
		}

		@Override
		protected long startTime() {
			return this.startTime;
		}

		@Override
		protected Long processUptime() {
			return this.uptime;
		}

		@Override
		protected String action() {
			return this.action;
		}

	}

}
