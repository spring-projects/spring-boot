/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.ProgressUpdateEvent.ProgressDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProgressUpdateEvent}.
 *
 * @param <E> The event type
 * @author Phillip Webb
 * @author Scott Frederick
 */
abstract class ProgressUpdateEventTests<E extends ProgressUpdateEvent> {

	@Test
	void getStatusReturnsStatus() {
		ProgressUpdateEvent event = createEvent();
		assertThat(event.getStatus()).isEqualTo("status");
	}

	@Test
	void getProgressDetailsReturnsProgressDetails() {
		ProgressUpdateEvent event = createEvent();
		assertThat(event.getProgressDetail().getCurrent()).isEqualTo(1);
		assertThat(event.getProgressDetail().getTotal()).isEqualTo(2);
	}

	@Test
	void getProgressReturnsProgress() {
		ProgressUpdateEvent event = createEvent();
		assertThat(event.getProgress()).isEqualTo("progress");
	}

	@Test
	void progressDetailIsEmptyWhenCurrentIsNullReturnsTrue() {
		ProgressDetail detail = new ProgressDetail(null, 2);
		assertThat(ProgressDetail.isEmpty(detail)).isTrue();
	}

	@Test
	void progressDetailIsEmptyWhenTotalIsNullReturnsTrue() {
		ProgressDetail detail = new ProgressDetail(1, null);
		assertThat(ProgressDetail.isEmpty(detail)).isTrue();
	}

	@Test
	void progressDetailIsEmptyWhenTotalAndCurrentAreNotNullReturnsFalse() {
		ProgressDetail detail = new ProgressDetail(1, 2);
		assertThat(ProgressDetail.isEmpty(detail)).isFalse();
	}

	protected E createEvent() {
		return createEvent("status", new ProgressDetail(1, 2), "progress");
	}

	protected abstract E createEvent(String status, ProgressDetail progressDetail, String progress);

}
