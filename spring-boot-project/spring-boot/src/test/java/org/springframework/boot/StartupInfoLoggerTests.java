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

package org.springframework.boot;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.system.ApplicationPid;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Test
	void startingFormat() {
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass()).logStarting(this.log);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		then(this.log).should().info(captor.capture());
		assertThat(captor.getValue().toString()).contains("Starting " + getClass().getSimpleName() + " using Java "
				+ System.getProperty("java.version") + " with PID " + new ApplicationPid() + " (started by "
				+ System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")");
	}

	@Test
	void startingFormatInAotMode() {
		System.setProperty("spring.aot.enabled", "true");
		try {
			given(this.log.isInfoEnabled()).willReturn(true);
			new StartupInfoLogger(getClass()).logStarting(this.log);
			ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
			then(this.log).should().info(captor.capture());
			assertThat(captor.getValue().toString())
				.contains("Starting AOT-processed " + getClass().getSimpleName() + " using Java "
						+ System.getProperty("java.version") + " with PID " + new ApplicationPid() + " (started by "
						+ System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")");

		}
		finally {
			System.clearProperty("spring.aot.enabled");
		}
	}

	@Test
	void startedFormat() {
		given(this.log.isInfoEnabled()).willReturn(true);
		Duration timeTakenToStartup = Duration.ofMillis(10);
		new StartupInfoLogger(getClass()).logStarted(this.log, timeTakenToStartup);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		then(this.log).should().info(captor.capture());
		assertThat(captor.getValue().toString()).matches("Started " + getClass().getSimpleName()
				+ " in \\d+\\.\\d{1,3} seconds \\(process running for \\d+\\.\\d{1,3}\\)");
	}

}
