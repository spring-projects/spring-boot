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

package org.springframework.boot.buildpack.platform.build;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.LogUpdateEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrintStreamBuildLog}.
 *
 * @author Phillip Webb
 * @author Rafael Ceccone
 */
class PrintStreamBuildLogTests {

	@Test
	void printsExpectedOutput() throws Exception {
		TestPrintStream out = new TestPrintStream();
		PrintStreamBuildLog log = new PrintStreamBuildLog(out);
		BuildRequest request = mock(BuildRequest.class);
		ImageReference name = ImageReference.of("my-app:latest");
		ImageReference builderImageReference = ImageReference.of("cnb/builder");
		Image builderImage = mock(Image.class);
		given(builderImage.getDigests()).willReturn(Collections.singletonList("00000001"));
		ImageReference runImageReference = ImageReference.of("cnb/runner");
		Image runImage = mock(Image.class);
		given(runImage.getDigests()).willReturn(Collections.singletonList("00000002"));
		given(request.getName()).willReturn(name);
		ImageReference tag = ImageReference.of("my-app:1.0");
		given(request.getTags()).willReturn(Collections.singletonList(tag));
		log.start(request);
		Consumer<TotalProgressEvent> pullBuildImageConsumer = log.pullingImage(builderImageReference,
				ImageType.BUILDER);
		pullBuildImageConsumer.accept(new TotalProgressEvent(100));
		log.pulledImage(builderImage, ImageType.BUILDER);
		Consumer<TotalProgressEvent> pullRunImageConsumer = log.pullingImage(runImageReference, ImageType.RUNNER);
		pullRunImageConsumer.accept(new TotalProgressEvent(100));
		log.pulledImage(runImage, ImageType.RUNNER);
		log.executingLifecycle(request, LifecycleVersion.parse("0.5"), VolumeName.of("pack-abc.cache"));
		Consumer<LogUpdateEvent> phase1Consumer = log.runningPhase(request, "alphabet");
		phase1Consumer.accept(mockLogEvent("one"));
		phase1Consumer.accept(mockLogEvent("two"));
		phase1Consumer.accept(mockLogEvent("three"));
		Consumer<LogUpdateEvent> phase2Consumer = log.runningPhase(request, "basket");
		phase2Consumer.accept(mockLogEvent("spring"));
		phase2Consumer.accept(mockLogEvent("boot"));
		log.executedLifecycle(request);
		log.taggedImage(tag);
		String expected = FileCopyUtils.copyToString(new InputStreamReader(
				getClass().getResourceAsStream("print-stream-build-log.txt"), StandardCharsets.UTF_8));
		assertThat(out.toString()).isEqualToIgnoringNewLines(expected);
	}

	private LogUpdateEvent mockLogEvent(String string) {
		LogUpdateEvent event = mock(LogUpdateEvent.class);
		given(event.toString()).willReturn(string);
		return event;
	}

	static class TestPrintStream extends PrintStream {

		TestPrintStream() {
			super(new ByteArrayOutputStream());
		}

		@Override
		public String toString() {
			return this.out.toString();
		}

	}

}
