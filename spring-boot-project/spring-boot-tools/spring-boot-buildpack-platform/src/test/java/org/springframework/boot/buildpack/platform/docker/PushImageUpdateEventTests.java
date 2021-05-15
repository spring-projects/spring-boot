/*
 * Copyright 2012-2020 the original author or authors.
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
 * Tests for {@link PushImageUpdateEvent}.
 *
 * @author Scott Frederick
 */
class PushImageUpdateEventTests extends ProgressUpdateEventTests<PushImageUpdateEvent> {

	@Test
	void getIdReturnsId() {
		PushImageUpdateEvent event = createEvent();
		assertThat(event.getId()).isEqualTo("id");
	}

	@Test
	void getErrorReturnsErrorDetail() {
		PushImageUpdateEvent event = new PushImageUpdateEvent(null, null, null, null,
				new PushImageUpdateEvent.ErrorDetail("test message"));
		assertThat(event.getErrorDetail().getMessage()).isEqualTo("test message");
	}

	@Override
	protected PushImageUpdateEvent createEvent(String status, ProgressDetail progressDetail, String progress) {
		return new PushImageUpdateEvent("id", status, progressDetail, progress, null);
	}

}
