/*
 * Copyright 2012-2019 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StartupInfoLogger}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class StartupInfoLoggerTests {

	private final Log log = mock(Log.class);

	@Test
	void sourceClassIncluded() {
		given(this.log.isInfoEnabled()).willReturn(true);
		new StartupInfoLogger(getClass()).logStarting(this.log);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(this.log).info(captor.capture());
		assertThat(captor.getValue().toString()).startsWith("Starting " + getClass().getSimpleName());
	}

	@Test
	void startedFormat() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		given(this.log.isInfoEnabled()).willReturn(true);
		stopWatch.stop();
		new StartupInfoLogger(getClass()).logStarted(this.log, stopWatch);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(this.log).info(captor.capture());
		assertThat(captor.getValue().toString()).matches("Started " + getClass().getSimpleName()
				+ " in \\d+\\.\\d{1,3} seconds \\(JVM running for \\d+\\.\\d{1,3}\\)");
	}

}
