/*
 * Copyright 2012-present the original author or authors.
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
 * @author Wolfgang Kronberg
 */
abstract class ProgressUpdateEventTests<E extends ProgressUpdateEvent> {

	@Test
	void getStatusReturnsStatus() {
		ProgressUpdateEvent event = createEvent();
		assertThat(event.getStatus()).isEqualTo("status");
	}

	@Test
	void getProgressDetailReturnsProgressDetails() {
		ProgressUpdateEvent event = createEvent();
		ProgressDetail progressDetail = event.getProgressDetail();
		assertThat(progressDetail).isNotNull();
		assertThat(progressDetail.asPercentage()).isEqualTo(50);
	}

	@Test
	void getProgressDetailReturnsProgressDetailsForLongNumbers() {
		ProgressUpdateEvent event = createEvent("status", new ProgressDetail(4000000000L, 8000000000L), "progress");
		ProgressDetail progressDetail = event.getProgressDetail();
		assertThat(progressDetail).isNotNull();
		assertThat(progressDetail.asPercentage()).isEqualTo(50);
	}

	@Test
	void getProgressReturnsProgress() {
		ProgressUpdateEvent event = createEvent();
		assertThat(event.getProgress()).isEqualTo("progress");
	}

	protected E createEvent() {
		return createEvent("status", new ProgressDetail(1L, 2L), "progress");
	}

	protected abstract E createEvent(String status, ProgressDetail progressDetail, String progress);

}
