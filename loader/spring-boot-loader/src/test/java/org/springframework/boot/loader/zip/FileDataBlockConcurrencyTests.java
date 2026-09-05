/*
 * Copyright 2012-present the original author or authors.
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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Concurrency stress test for {@link FileDataBlock}. Hammers
 * {@link FileDataBlock#open()}, {@link FileDataBlock#read(ByteBuffer, long)}, and
 * {@link FileDataBlock#close()} from many threads at once to prove that reference
 * counting and reads remain correct under load, and reports achieved throughput.
 *
 * <p>
 * This intentionally does not assert on wall-clock timing, since a fixed threshold would
 * be flaky in CI. Instead it prints throughput so implementations can be compared
 * manually, for example by running this test against a commit with only the
 * {@code NestedJarFile} fix applied, and again with the {@code FileDataBlock} locking
 * change applied as well.
 *
 * @author Ian Kettle
 */
class FileDataBlockConcurrencyTests {

	private static final byte[] CONTENT = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

	private static final int THREAD_COUNT = 32;

	private static final int ITERATIONS_PER_THREAD = 5_000;

	@TempDir
	File tempDir;

	@Test
	void concurrentOpenReadCloseIsSafeAndReportsThroughput() throws Exception {
		File file = new File(this.tempDir, "content");
		Files.write(file.toPath(), CONTENT);
		FileDataBlock block = new FileDataBlock(file.toPath());
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT,
				FileDataBlockConcurrencyTests::newDaemonThread);
		CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<AssertionError> failure = new AtomicReference<>();
		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			futures.add(executor.submit(hammer(block, ready, start, failure)));
		}
		ready.await();
		long startNanos = System.nanoTime();
		start.countDown();
		awaitAll(futures);
		long elapsedNanos = System.nanoTime() - startNanos;
		executor.shutdownNow();
		if (failure.get() != null) {
			throw failure.get();
		}
		reportThroughput(elapsedNanos);
		// Every open() must have been matched by a close(); a further read must fail
		// cleanly rather than return stale or corrupted data.
		assertThatExceptionOfType(ClosedChannelException.class).isThrownBy(() -> block.read(ByteBuffer.allocate(1), 0));
	}

	private Callable<Void> hammer(FileDataBlock block, CountDownLatch ready, CountDownLatch start,
			AtomicReference<AssertionError> failure) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			for (int i = 0; i < ITERATIONS_PER_THREAD && failure.get() == null; i++) {
				block.open();
				try {
					buffer.clear();
					int read = block.read(buffer, 0);
					if (read != CONTENT.length || !Arrays.equals(buffer.array(), CONTENT)) {
						failure.compareAndSet(null, new AssertionError("Corrupted read: count=%d content=%s"
							.formatted(read, Arrays.toString(buffer.array()))));
					}
				}
				finally {
					block.close();
				}
			}
			return null;
		};
	}

	private void awaitAll(List<Future<Void>> futures) throws InterruptedException {
		List<Throwable> failures = new ArrayList<>();
		for (Future<Void> future : futures) {
			try {
				future.get(60, TimeUnit.SECONDS);
			}
			catch (TimeoutException ex) {
				failures.add(new AssertionError(
						"Timed out waiting for concurrent access to complete - possible deadlock", ex));
			}
			catch (ExecutionException ex) {
				failures.add(ex.getCause());
			}
		}
		if (!failures.isEmpty()) {
			AssertionError combined = new AssertionError(failures.size() + " thread(s) failed:\n"
					+ failures.stream().map(Throwable::toString).collect(Collectors.joining("\n")));
			failures.forEach(combined::addSuppressed);
			throw combined;
		}
	}

	private void reportThroughput(long elapsedNanos) {
		long totalOperations = (long) THREAD_COUNT * ITERATIONS_PER_THREAD;
		double seconds = elapsedNanos / 1_000_000_000.0;
		System.out.printf(
				"FileDataBlock concurrent open/read/close: %d threads x %d iterations = %d ops in %.3fs (%.0f ops/sec)%n",
				THREAD_COUNT, ITERATIONS_PER_THREAD, totalOperations, seconds, totalOperations / seconds);
	}

	private static void awaitUninterruptibly(CountDownLatch latch) {
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private static Thread newDaemonThread(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.setDaemon(true);
		return thread;
	}

}
