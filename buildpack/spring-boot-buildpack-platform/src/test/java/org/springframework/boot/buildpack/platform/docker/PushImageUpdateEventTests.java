/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.ProgressUpdateEvent.ProgressDetail;
import org.springframework.boot.buildpack.platform.docker.PushImageUpdateEvent.ErrorDetail;

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
		PushImageUpdateEvent event = new PushImageUpdateEvent("id", "status", new ProgressDetail(null, null),
				"progress", new PushImageUpdateEvent.ErrorDetail("test message"));
		ErrorDetail errorDetail = event.getErrorDetail();
		assertThat(errorDetail).isNotNull();
		assertThat(errorDetail.getMessage()).isEqualTo("test message");
	}

	@Override
	protected PushImageUpdateEvent createEvent(String status, ProgressDetail progressDetail, String progress) {
		return new PushImageUpdateEvent("id", status, progressDetail, progress, null);
	}

}
