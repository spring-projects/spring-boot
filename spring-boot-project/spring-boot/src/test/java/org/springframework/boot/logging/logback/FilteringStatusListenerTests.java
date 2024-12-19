/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.WarnStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilteringStatusListener}.
 *
 * @author Dmytro Nosan
 */
class FilteringStatusListenerTests {

	private final DelegateStatusListener delegate = new DelegateStatusListener();

	@Test
	void shouldFilterOutInfoStatus() {
		FilteringStatusListener listener = createListener(Status.WARN);
		InfoStatus info = new InfoStatus("info", getClass());
		WarnStatus warn = new WarnStatus("warn", getClass());
		ErrorStatus error = new ErrorStatus("error", getClass());
		listener.addStatusEvent(info);
		listener.addStatusEvent(warn);
		listener.addStatusEvent(error);
		assertThat(this.delegate.getStatuses()).containsExactly(warn, error);
	}

	@Test
	void shouldStartUnderlyingStatusListener() {
		FilteringStatusListener listener = createListener(Status.INFO);
		assertThat(this.delegate.isStarted()).isFalse();
		listener.start();
		assertThat(this.delegate.isStarted()).isTrue();
	}

	@Test
	void shouldStopUnderlyingStatusListener() {
		FilteringStatusListener listener = createListener();
		this.delegate.start();
		assertThat(this.delegate.isStarted()).isTrue();
		listener.stop();
		assertThat(this.delegate.isStarted()).isFalse();
	}

	@Test
	void shouldUseResetResistantValueFromUnderlyingStatusListener() {
		FilteringStatusListener listener = createListener();
		assertThat(listener.isResetResistant()).isEqualTo(this.delegate.isResetResistant());
	}

	private FilteringStatusListener createListener() {
		return new FilteringStatusListener(this.delegate, Status.INFO);
	}

	private FilteringStatusListener createListener(int levelThreshold) {
		return new FilteringStatusListener(this.delegate, levelThreshold);
	}

	private static final class DelegateStatusListener implements StatusListener, LifeCycle {

		private final List<Status> statuses = new ArrayList<>();

		private boolean started = false;

		@Override
		public void addStatusEvent(Status status) {
			this.statuses.add(status);
		}

		List<Status> getStatuses() {
			return this.statuses;
		}

		@Override
		public boolean isResetResistant() {
			return true;
		}

		@Override
		public void start() {
			this.started = true;
		}

		@Override
		public void stop() {
			this.started = false;
		}

		@Override
		public boolean isStarted() {
			return this.started;
		}

	}

}
