/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.zip;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.loader.ref.DefaultCleanerTracking;
import org.springframework.boot.loader.zip.FileDataBlock.Tracker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension for {@link AssertFileChannelDataBlocksClosed @TrackFileChannelDataBlock}.
 */
class AssertFileChannelDataBlocksClosedExtension implements BeforeEachCallback, AfterEachCallback {

	private static OpenFilesTracker tracker = new OpenFilesTracker();

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		tracker.clear();
		FileDataBlock.tracker = tracker;
		DefaultCleanerTracking.set(tracker::addedCleanable);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		tracker.assertAllClosed();
		FileDataBlock.tracker = Tracker.NONE;
	}

	private static final class OpenFilesTracker implements Tracker {

		private final Set<Path> paths = new LinkedHashSet<>();

		private final List<Cleanable> clean = new ArrayList<>();

		private final List<Closeable> close = new ArrayList<>();

		@Override
		public void openedFileChannel(Path path) {
			this.paths.add(path);
		}

		@Override
		public void closedFileChannel(Path path) {
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
