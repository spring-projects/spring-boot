/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.LoadImageUpdateEvent.ErrorDetail;
import org.springframework.boot.buildpack.platform.docker.ProgressUpdateEvent.ProgressDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadImageUpdateEvent}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class LoadImageUpdateEventTests extends ProgressUpdateEventTests<LoadImageUpdateEvent> {

	@Test
	void getStreamReturnsStream() {
		LoadImageUpdateEvent event = createEvent();
		assertThat(event.getStream()).isEqualTo("stream");
	}

	@Test
	void getErrorDetailReturnsErrorDetail() {
		LoadImageUpdateEvent event = createEvent();
		assertThat(event.getErrorDetail()).extracting(ErrorDetail::getMessage).isEqualTo("max depth exceeded");
	}

	@Override
	protected LoadImageUpdateEvent createEvent(String status, ProgressDetail progressDetail, String progress) {
		return new LoadImageUpdateEvent("stream", status, progressDetail, progress,
				new ErrorDetail("max depth exceeded"));
	}

}
