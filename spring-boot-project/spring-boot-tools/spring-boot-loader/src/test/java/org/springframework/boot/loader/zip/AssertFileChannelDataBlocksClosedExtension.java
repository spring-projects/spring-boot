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

package org.springframework.boot.loader.zip;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.loader.ref.DefaultCleanerTracking;
import org.springframework.boot.loader.zip.FileChannelDataBlock.Tracker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension for {@link AssertFileChannelDataBlocksClosed @TrackFileChannelDataBlock}.
 */
class AssertFileChannelDataBlocksClosedExtension implements BeforeEachCallback, AfterEachCallback {

	private static OpenFilesTracker tracker = new OpenFilesTracker();

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		tracker.clear();
		FileChannelDataBlock.tracker = tracker;
		DefaultCleanerTracking.set(tracker::addedCleanable);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		tracker.assertAllClosed();
		FileChannelDataBlock.tracker = null;
	}

	private static class OpenFilesTracker implements Tracker {

		private final Set<Path> paths = new LinkedHashSet<>();

		private final List<Cleanable> clean = new ArrayList<>();

		private final List<Closeable> close = new ArrayList<>();

		@Override
		public void openedFileChannel(Path path, FileChannel fileChannel) {
			this.paths.add(path);
		}

		@Override
		public void closedFileChannel(Path path, FileChannel fileChannel) {
			this.paths.remove(path);
		}

		void clear() {
			this.paths.clear();
			this.clean.clear();
		}

		void assertAllClosed() throws IOException {
			for (Closeable closeable : this.close) {
				closeable.close();
			}
			this.clean.forEach(Cleanable::clean);
			assertThat(this.paths).as("open paths").isEmpty();
		}

		private void addedCleanable(Object obj, Cleanable cleanable) {
			if (cleanable != null) {
				this.clean.add(cleanable);
			}
			if (obj instanceof Closeable closeable) {
				this.close.add(closeable);
			}
		}

	}

}
