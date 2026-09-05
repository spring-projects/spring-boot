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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;

/**
 * Concurrency stress test for {@link NestedJarFile}. Hammers the read paths that no
 * longer synchronize on {@code this} - {@link NestedJarFile#hasEntry(String)},
 * {@link NestedJarFile#getJarEntry(String)}, {@link NestedJarFile#getComment()},
 * {@link NestedJarFile#entries()}, and {@link NestedJarFile#stream()} - alongside
 * {@link NestedJarFile#getInputStream(JarEntry)} (which remains synchronized) from many
 * threads at once, to prove that removing synchronization from those methods has not
 * introduced any read corruption, and to report achieved throughput.
 *
 * <p>
 * This intentionally does not assert on wall-clock timing, since a fixed threshold would
 * be flaky in CI. Instead it prints throughput so implementations can be compared
 * manually, for example against a commit that still synchronizes on {@code this} for
 * these methods.
 *
 * @author Ian Kettle
 */
class NestedJarFileConcurrencyTests {

	private static final int THREAD_COUNT = 32;

	private static final int ITERATIONS_PER_THREAD = 2_000;

	private static final int CLOSE_RACE_TRIALS = 200;

	private static final int CLOSE_RACE_READER_COUNT = 8;

	private static final int CLOSE_RACE_MAX_ITERATIONS_PER_READER = 500;

	@TempDir
	File tempDir;

	private NestedJarFile jarFile;

	@AfterEach
	void closeJarFile() throws Exception {
		if (this.jarFile != null) {
			this.jarFile.close();
		}
	}

	@Test
	void concurrentReadsAreSafeAndReportThroughput() throws Exception {
		File file = new File(this.tempDir, "test.jar");
		TestJar.create(file);
		this.jarFile = new NestedJarFile(file);
		int expectedEntryCount = TestJar.expectedEntries().size();
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT,
				NestedJarFileConcurrencyTests::newDaemonThread);
		CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<AssertionError> failure = new AtomicReference<>();
		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			futures.add(executor.submit(hammer(ready, start, failure, expectedEntryCount)));
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
	}

	/**
	 * Targets the specific race between {@link NestedJarFile#close()} and
	 * {@code ensureOpen()}: {@code closed} and {@code NestedJarFileResources.zipContent}
	 * are read as two separate, non-atomic steps, and {@code zipContent} is not
	 * {@code volatile}. Repeatedly races readers against a concurrent {@code close()} and
	 * asserts that the only possible outcomes are a successful read (if it completes
	 * before the close is observed) or a clean failure ({@link IllegalStateException},
	 * {@link IOException}, or {@link NoSuchElementException}) - never corruption, an
	 * unexpected exception type, or a hang. A single trial is unlikely to hit the exact
	 * race window, so this runs many independent trials against fresh
	 * {@link NestedJarFile} instances.
	 */
	@Test
	void concurrentCloseDuringReadsOnlyProducesCleanFailures() throws Exception {
		File file = new File(this.tempDir, "close-race.jar");
		TestJar.create(file);
		for (int trial = 0; trial < CLOSE_RACE_TRIALS; trial++) {
			runCloseRaceTrial(file);
		}
	}

	private void runCloseRaceTrial(File file) throws Exception {
		NestedJarFile jar = new NestedJarFile(file);
		ExecutorService executor = Executors.newFixedThreadPool(CLOSE_RACE_READER_COUNT,
				NestedJarFileConcurrencyTests::newDaemonThread);
		CountDownLatch ready = new CountDownLatch(CLOSE_RACE_READER_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<AssertionError> failure = new AtomicReference<>();
		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < CLOSE_RACE_READER_COUNT; i++) {
			futures.add(executor.submit(readUntilClosedOrLimit(jar, ready, start, failure)));
		}
		ready.await();
		start.countDown();
		// Close as early as possible to maximise the chance of racing a reader's
		// ensureOpen() check against the non-atomic closed/zipContent state.
		jar.close();
		try {
			awaitAll(futures);
		}
		finally {
			executor.shutdownNow();
		}
		if (failure.get() != null) {
			throw failure.get();
		}
	}

	private Callable<Void> readUntilClosedOrLimit(NestedJarFile jar, CountDownLatch ready, CountDownLatch start,
			AtomicReference<AssertionError> failure) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			for (int i = 0; i < CLOSE_RACE_MAX_ITERATIONS_PER_READER && failure.get() == null; i++) {
				try {
					jar.hasEntry("1.dat");
					JarEntry entry = jar.getJarEntry("1.dat");
					if (entry != null) {
						try (InputStream inputStream = jar.getInputStream(entry)) {
							inputStream.readAllBytes();
						}
					}
					jar.getComment();
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						entries.nextElement();
					}
				}
				catch (Exception ex) {
					if (!isCleanCloseFailure(ex)) {
						failure.compareAndSet(null,
								new AssertionError("Unexpected failure during close race: " + ex, ex));
					}
					// A clean "closed" signal - the jar is gone, no point looping
					// further.
					return null;
				}
			}
			return null;
		};
	}

	private boolean isCleanCloseFailure(Throwable ex) {
		for (Throwable current = ex; current != null; current = current.getCause()) {
			if (current instanceof IllegalStateException || current instanceof IOException
					|| current instanceof NoSuchElementException) {
				return true;
			}
		}
		return false;
	}

	private Callable<Void> hammer(CountDownLatch ready, CountDownLatch start, AtomicReference<AssertionError> failure,
			int expectedEntryCount) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			for (int i = 0; i < ITERATIONS_PER_THREAD && failure.get() == null; i++) {
				checkHasEntry(failure);
				checkGetJarEntryAndReadContent(failure);
				checkGetComment(failure);
				checkEntries(failure, expectedEntryCount);
				checkStream(failure, expectedEntryCount);
			}
			return null;
		};
	}

	private void checkHasEntry(AtomicReference<AssertionError> failure) {
		try {
			if (!this.jarFile.hasEntry("1.dat")) {
				failure.compareAndSet(null, new AssertionError("hasEntry('1.dat') returned false"));
			}
		}
		catch (RuntimeException ex) {
			failure.compareAndSet(null, new AssertionError("hasEntry threw", ex));
		}
	}

	private void checkGetJarEntryAndReadContent(AtomicReference<AssertionError> failure) {
		try {
			JarEntry entry = this.jarFile.getJarEntry("1.dat");
			if (entry == null) {
				failure.compareAndSet(null, new AssertionError("getJarEntry('1.dat') returned null"));
				return;
			}
			try (InputStream inputStream = this.jarFile.getInputStream(entry)) {
				byte[] content = inputStream.readAllBytes();
				if (content.length != 1 || content[0] != 1) {
					failure.compareAndSet(null,
							new AssertionError("Corrupted content for '1.dat': " + Arrays.toString(content)));
				}
			}
		}
		catch (Exception ex) {
			failure.compareAndSet(null, new AssertionError("getJarEntry/getInputStream threw", ex));
		}
	}

	private void checkGetComment(AtomicReference<AssertionError> failure) {
		try {
			String comment = this.jarFile.getComment();
			if (!"outer".equals(comment)) {
				failure.compareAndSet(null, new AssertionError("Unexpected comment: " + comment));
			}
		}
		catch (RuntimeException ex) {
			failure.compareAndSet(null, new AssertionError("getComment threw", ex));
		}
	}

	private void checkEntries(AtomicReference<AssertionError> failure, int expectedEntryCount) {
		try {
			Enumeration<JarEntry> entries = this.jarFile.entries();
			int count = 0;
			while (entries.hasMoreElements()) {
				entries.nextElement();
				count++;
			}
			if (count != expectedEntryCount) {
				failure.compareAndSet(null,
						new AssertionError("Expected " + expectedEntryCount + " entries but got " + count));
			}
		}
		catch (RuntimeException ex) {
			failure.compareAndSet(null, new AssertionError("entries() iteration threw", ex));
		}
	}

	private void checkStream(AtomicReference<AssertionError> failure, int expectedEntryCount) {
		try {
			long count = this.jarFile.stream().count();
			if (count != expectedEntryCount) {
				failure.compareAndSet(null,
						new AssertionError("Expected " + expectedEntryCount + " entries but stream had " + count));
			}
		}
		catch (RuntimeException ex) {
			failure.compareAndSet(null, new AssertionError("stream() threw", ex));
		}
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
		long operationsPerIteration = 5;
		long totalOperations = (long) THREAD_COUNT * ITERATIONS_PER_THREAD * operationsPerIteration;
		double seconds = elapsedNanos / 1_000_000_000.0;
		System.out.printf(
				"NestedJarFile concurrent reads: %d threads x %d iterations x %d ops = %d ops in %.3fs (%.0f ops/sec)%n",
				THREAD_COUNT, ITERATIONS_PER_THREAD, operationsPerIteration, totalOperations, seconds,
				totalOperations / seconds);
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
