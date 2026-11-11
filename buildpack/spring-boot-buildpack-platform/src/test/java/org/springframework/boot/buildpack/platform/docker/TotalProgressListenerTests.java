/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.boot.buildpack.platform.json.JsonStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TotalProgressPullListener}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class TotalProgressListenerTests extends AbstractJsonTests {

	@Test
	void totalProgress() throws Exception {
		List<Integer> progress = new ArrayList<>();
		TestTotalProgressListener listener = new TestTotalProgressListener((event) -> progress.add(event.getPercent()));
		run(listener);
		int last = 0;
		for (Integer update : progress) {
			assertThat(update).isGreaterThanOrEqualTo(last);
			last = update;
		}
		assertThat(last).isEqualTo(100);
	}

	@Test
	@Disabled("For visual inspection")
	void totalProgressUpdatesSmoothly() throws Exception {
		TestTotalProgressListener listener = new TestTotalProgressListener(new TotalProgressBar("Pulling layers:"));
		run(listener);
	}

	private void run(TestTotalProgressListener listener) throws IOException {
		JsonStream jsonStream = new JsonStream(getJsonMapper());
		listener.onStart();
		jsonStream.get(getContent("pull-stream.json"), TestImageUpdateEvent.class, listener::onUpdate);
		listener.onFinish();
	}

	private static class TestTotalProgressListener extends TotalProgressListener<TestImageUpdateEvent> {

		TestTotalProgressListener(Consumer<TotalProgressEvent> consumer) {
			super(consumer, new String[] { "Pulling", "Downloading", "Extracting" });
		}

		@Override
		public void onUpdate(TestImageUpdateEvent event) {
			super.onUpdate(event);
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException ex) {
				// Ignore
			}
		}

	}

	private static class TestImageUpdateEvent extends ImageProgressUpdateEvent {

		@JsonCreator
		TestImageUpdateEvent(String id, String status, ProgressDetail progressDetail, String progress) {
			super(id, status, progressDetail, progress);
		}

	}

}
