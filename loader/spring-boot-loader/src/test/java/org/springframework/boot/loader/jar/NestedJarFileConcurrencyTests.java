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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;

import static org.assertj.core.api.Assertions.assertThat;

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
 * <p>
 * Alongside the throughput run there are three races against {@code close()}: readers
 * against a concurrent {@code close()}, a {@code close()} landing mid-stream, and a
 * {@code close()} of a second jar that shares the same reference counted
 * {@link org.springframework.boot.loader.zip.ZipContent}.
 *
 * @author Ian Kettle
 */
class NestedJarFileConcurrencyTests {

	private static final int THREAD_COUNT = 32;

	private static final int ITERATIONS_PER_THREAD = 2_000;

	private static final int CLOSE_RACE_TRIALS = 200;

	private static final int CLOSE_RACE_READER_COUNT = 8;

	private static final int CLOSE_RACE_MAX_ITERATIONS_PER_READER = 500;

	private static final int STREAM_RACE_TRIALS = 200;

	private static final int INFLATER_RACE_TRIALS = 200;

	private static final int INFLATER_RACE_READER_COUNT = 8;

	private static final int INFLATER_RACE_MAX_ITERATIONS_PER_READER = 500;

	private static final int SHARED_ZIP_CONTENT_READER_COUNT = 4;

	private static final int SHARED_ZIP_CONTENT_CHURN_ITERATIONS = 200;

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
	 * asserts that the only possible outcomes are a successful read <em>returning the
	 * correct content</em> (if it completes before the close is observed) or a clean
	 * failure ({@link IllegalStateException}, {@link IOException}, or
	 * {@link NoSuchElementException}) - never corruption, an unexpected exception type,
	 * or a hang. A single trial is unlikely to hit the exact race window, so this runs
	 * many independent trials against fresh {@link NestedJarFile} instances.
	 * <p>
	 * Both outcomes are counted across all trials and asserted on at the end. Without
	 * that, the test would pass vacuously whenever {@code close()} consistently won or
	 * consistently lost the race - which is easy to do accidentally, since simply calling
	 * {@code close()} straight after releasing the readers means they almost always
	 * observe {@code closed == true} on their very first check and the interesting
	 * {@code zipContent}/reference-count window is never entered at all.
	 */
	@Test
	void concurrentCloseDuringReadsOnlyProducesCleanFailures() throws Exception {
		File file = new File(this.tempDir, "close-race.jar");
		TestJar.create(file);
		int expectedEntryCount = TestJar.expectedEntries().size();
		AtomicLong successfulReads = new AtomicLong();
		AtomicLong closedSignals = new AtomicLong();
		for (int trial = 0; trial < CLOSE_RACE_TRIALS; trial++) {
			runCloseRaceTrial(file, trial, expectedEntryCount, successfulReads, closedSignals);
		}
		assertThat(successfulReads).as("reads that completed before close() was observed").hasValueGreaterThan(0L);
		assertThat(closedSignals).as("reads that raced close() and failed cleanly").hasValueGreaterThan(0L);
	}

	private void runCloseRaceTrial(File file, int trial, int expectedEntryCount, AtomicLong successfulReads,
			AtomicLong closedSignals) throws Exception {
		NestedJarFile jar = new NestedJarFile(file);
		ExecutorService executor = Executors.newFixedThreadPool(CLOSE_RACE_READER_COUNT,
				NestedJarFileConcurrencyTests::newDaemonThread);
		CountDownLatch ready = new CountDownLatch(CLOSE_RACE_READER_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch warmedUp = new CountDownLatch(CLOSE_RACE_READER_COUNT);
		AtomicReference<AssertionError> failure = new AtomicReference<>();
		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < CLOSE_RACE_READER_COUNT; i++) {
			futures.add(executor.submit(readUntilClosedOrLimit(jar, ready, start, warmedUp, failure, expectedEntryCount,
					successfulReads, closedSignals)));
		}
		ready.await();
		start.countDown();
		// Wait until every reader has completed one full pass so that at least one
		// success is guaranteed, then vary how long we wait before closing so that
		// close() lands at a different point in the read path on each trial rather
		// than always winning the race outright.
		warmedUp.await(30, TimeUnit.SECONDS);
		spin(trial % 97);
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
			CountDownLatch warmedUp, AtomicReference<AssertionError> failure, int expectedEntryCount,
			AtomicLong successfulReads, AtomicLong closedSignals) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			for (int i = 0; i < CLOSE_RACE_MAX_ITERATIONS_PER_READER && failure.get() == null; i++) {
				try {
					readEverything(jar, expectedEntryCount);
					successfulReads.incrementAndGet();
				}
				catch (Exception ex) {
					if (!isCleanCloseFailure(ex)) {
						failure.compareAndSet(null,
								new AssertionError("Unexpected failure during close race: " + describe(ex), ex));
					}
					else {
						closedSignals.incrementAndGet();
					}
					// A clean "closed" signal - the jar is gone, no point looping
					// further.
					return null;
				}
				finally {
					if (i == 0) {
						warmedUp.countDown();
					}
				}
			}
			return null;
		};
	}

	/**
	 * Exercise the read paths that no longer synchronize on {@code this}, asserting on
	 * the content of everything that is read. A read that races {@code close()} is
	 * allowed to fail, but a read that <em>succeeds</em> must return the real data -
	 * otherwise a torn or stale read would go unnoticed. Failures here are
	 * {@link AssertionError}s rather than exceptions, so they bypass the clean-failure
	 * filter in the caller.
	 */
	private void readEverything(NestedJarFile jar, int expectedEntryCount) throws IOException {
		assertThat(jar.hasEntry("1.dat")).isTrue();
		JarEntry entry = jar.getJarEntry("1.dat");
		assertThat(entry).isNotNull();
		try (InputStream inputStream = jar.getInputStream(entry)) {
			assertThat(inputStream.readAllBytes()).containsExactly(1);
		}
		assertThat(jar.getComment()).isEqualTo("outer");
		Enumeration<JarEntry> entries = jar.entries();
		int count = 0;
		while (entries.hasMoreElements()) {
			entries.nextElement();
			count++;
		}
		assertThat(count).isEqualTo(expectedEntryCount);
		assertThat(jar.stream().count()).isEqualTo(expectedEntryCount);
	}

	/**
	 * Describe a failure including its cause chain and the frame each cause was thrown
	 * from. The JVM's {@code OmitStackTraceInFastThrow} optimization strips both the
	 * message and the stack trace from an implicit {@link NullPointerException} once it
	 * has been thrown from the same site often enough, which is exactly what happens in a
	 * loop like this one. Without the surrounding frames such a failure cannot be
	 * attributed to a particular race, so record whatever detail is available and say
	 * explicitly when there is none.
	 * @param ex the failure to describe
	 * @return a description suitable for an assertion message
	 */
	private String describe(Throwable ex) {
		StringBuilder description = new StringBuilder();
		for (Throwable current = ex; current != null; current = current.getCause()) {
			description.append((!description.isEmpty()) ? " <- " : "").append(current);
			StackTraceElement[] stackTrace = current.getStackTrace();
			description.append((stackTrace.length > 0) ? " at " + stackTrace[0]
					: " (no stack trace - run with -XX:-OmitStackTraceInFastThrow)");
		}
		return description.toString();
	}

	private boolean isCleanCloseFailure(Throwable ex) {
		for (Throwable current = ex; current != null; current = current.getCause()) {
			// A NullPointerException anywhere in the chain is never acceptable, even if
			// something further down the chain looks clean - it means a read reached a
			// nulled buffer, ZipContent or Inflater rather than being rejected.
			if (current instanceof NullPointerException) {
				return false;
			}
		}
		for (Throwable current = ex; current != null; current = current.getCause()) {
			if (current instanceof IllegalStateException || current instanceof IOException
					|| current instanceof NoSuchElementException) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Drives the {@code referenceCount == 0} guard in {@code FileDataBlock.FileAccess}
	 * via the public API by closing the jar while an entry is being streamed. Reading a
	 * byte at a time keeps the stream contending for {@code NestedJarFile}'s monitor -
	 * which {@link NestedJarFile#getInputStream(java.util.zip.ZipEntry)} deliberately
	 * still holds - so {@code close()} reliably lands mid-stream. Whatever was read
	 * before the failure must be a genuine prefix of the entry, never stale bytes from a
	 * buffer that was discarded and refilled underneath the reader.
	 * <p>
	 * A {@code STORED} entry is used on purpose. A {@code DEFLATED} entry would also
	 * exercise {@code JarEntryInflaterInputStream}, whose {@code inflate} call does not
	 * hold the jar monitor, so {@code close()} can end the {@link java.util.zip.Inflater}
	 * mid-inflation and produce a {@link NullPointerException} from
	 * {@code Inflater.ensureOpen()} rather than a clean {@link IOException}.
	 */
	@Test
	void closingWhileStreamingReturnsCompleteContentOrFailsCleanly() throws Exception {
		File file = new File(this.tempDir, "stream-race.jar");
		TestJar.create(file);
		byte[] expected = readEntryFully(file, "nested.jar");
		AtomicLong completed = new AtomicLong();
		AtomicLong closedMidStream = new AtomicLong();
		for (int trial = 0; trial < STREAM_RACE_TRIALS; trial++) {
			runStreamRaceTrial(file, expected, completed, closedMidStream);
		}
		assertThat(closedMidStream).as("streams interrupted by a concurrent close()").hasValueGreaterThan(0L);
		System.out.printf("NestedJarFile close during streaming: %d completed, %d interrupted cleanly%n",
				completed.get(), closedMidStream.get());
	}

	private void runStreamRaceTrial(File file, byte[] expected, AtomicLong completed, AtomicLong closedMidStream)
			throws Exception {
		ByteArrayOutputStream read = new ByteArrayOutputStream();
		NestedJarFile jar = new NestedJarFile(file);
		Thread closer = newDaemonThread(() -> {
			try {
				jar.close();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});
		try (InputStream inputStream = jar.getInputStream(jar.getJarEntry("nested.jar"))) {
			closer.start();
			int b;
			while ((b = inputStream.read()) != -1) {
				read.write(b);
			}
			completed.incrementAndGet();
		}
		catch (IOException ex) {
			closedMidStream.incrementAndGet();
		}
		finally {
			closer.join(30_000);
			jar.close();
		}
		byte[] actual = read.toByteArray();
		assertThat(actual).as("bytes read before the close was observed")
			.isEqualTo(Arrays.copyOf(expected, actual.length));
	}

	/**
	 * Targets {@code NestedJarFileResources}' inflater cache, which
	 * {@link NestedJarFile#getInputStream(java.util.zip.ZipEntry)} drives for every
	 * {@code DEFLATED} entry: opening a stream polls the cache and closing it returns the
	 * inflater. Neither happens under {@code NestedJarFile}'s monitor, so both race
	 * {@code releaseInflators()}. Every reader does nothing but open and close such a
	 * stream, which churns the cache far harder than the general close race does and so
	 * reproduces cache corruption much more reliably.
	 */
	@Test
	void concurrentStreamClosesDuringJarCloseDoNotCorruptTheInflaterCache() throws Exception {
		File file = new File(this.tempDir, "inflater-race.jar");
		TestJar.create(file);
		AtomicLong streamsOpened = new AtomicLong();
		AtomicLong closedSignals = new AtomicLong();
		for (int trial = 0; trial < INFLATER_RACE_TRIALS; trial++) {
			runInflaterRaceTrial(file, streamsOpened, closedSignals);
		}
		assertThat(streamsOpened).as("inflater streams opened and closed before close()").hasValueGreaterThan(0L);
		assertThat(closedSignals).as("streams that raced close() and failed cleanly").hasValueGreaterThan(0L);
	}

	private void runInflaterRaceTrial(File file, AtomicLong streamsOpened, AtomicLong closedSignals) throws Exception {
		NestedJarFile jar = new NestedJarFile(file);
		// '1.dat' is DEFLATED, so each stream borrows and returns a cached Inflater.
		JarEntry entry = jar.getJarEntry("1.dat");
		ExecutorService executor = Executors.newFixedThreadPool(INFLATER_RACE_READER_COUNT,
				NestedJarFileConcurrencyTests::newDaemonThread);
		CountDownLatch ready = new CountDownLatch(INFLATER_RACE_READER_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch warmedUp = new CountDownLatch(INFLATER_RACE_READER_COUNT);
		AtomicReference<AssertionError> failure = new AtomicReference<>();
		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < INFLATER_RACE_READER_COUNT; i++) {
			futures.add(executor.submit(openAndCloseInflaterStreams(jar, entry, ready, start, warmedUp, failure,
					streamsOpened, closedSignals)));
		}
		ready.await();
		start.countDown();
		warmedUp.await(30, TimeUnit.SECONDS);
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

	private Callable<Void> openAndCloseInflaterStreams(NestedJarFile jar, JarEntry entry, CountDownLatch ready,
			CountDownLatch start, CountDownLatch warmedUp, AtomicReference<AssertionError> failure,
			AtomicLong streamsOpened, AtomicLong closedSignals) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			for (int i = 0; i < INFLATER_RACE_MAX_ITERATIONS_PER_READER && failure.get() == null; i++) {
				try {
					try (InputStream inputStream = jar.getInputStream(entry)) {
						assertThat(inputStream.readAllBytes()).containsExactly(1);
					}
					streamsOpened.incrementAndGet();
				}
				catch (Exception ex) {
					if (!isCleanCloseFailure(ex)) {
						failure.compareAndSet(null, new AssertionError(
								"Unexpected failure during inflater cache race: " + describe(ex), ex));
					}
					else {
						closedSignals.incrementAndGet();
					}
					return null;
				}
				finally {
					if (i == 0) {
						warmedUp.countDown();
					}
				}
			}
			return null;
		};
	}

	private byte[] readEntryFully(File file, String name) throws Exception {
		try (NestedJarFile jar = new NestedJarFile(file)) {
			try (InputStream inputStream = jar.getInputStream(jar.getJarEntry(name))) {
				return inputStream.readAllBytes();
			}
		}
	}

	/**
	 * {@link org.springframework.boot.loader.zip.ZipContent} instances are cached per
	 * source and their {@code FileDataBlock} is reference counted, so closing one
	 * {@link NestedJarFile} must only decrement that count. This is also the case where
	 * the reasoning "a stale read of the non-volatile {@code zipContent} field always
	 * ends in a {@code ClosedChannelException}" does not hold: with another holder still
	 * open the stale reference is fully live and returns correct data. Either way no
	 * reader of the surviving jar may ever see a failure, so any exception at all is a
	 * bug here.
	 */
	@Test
	void closingOneJarDoesNotDisturbAnotherSharingTheSameZipContent() throws Exception {
		File file = new File(this.tempDir, "shared.jar");
		TestJar.create(file);
		int expectedEntryCount = TestJar.expectedEntries().size();
		this.jarFile = new NestedJarFile(file);
		ExecutorService executor = Executors.newFixedThreadPool(SHARED_ZIP_CONTENT_READER_COUNT + 1,
				NestedJarFileConcurrencyTests::newDaemonThread);
		CountDownLatch ready = new CountDownLatch(SHARED_ZIP_CONTENT_READER_COUNT + 1);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch churnFinished = new CountDownLatch(1);
		AtomicReference<AssertionError> failure = new AtomicReference<>();
		List<Future<Void>> futures = new ArrayList<>();
		futures.add(executor.submit(openAndCloseOtherJars(file, ready, start, churnFinished)));
		for (int i = 0; i < SHARED_ZIP_CONTENT_READER_COUNT; i++) {
			futures.add(executor
				.submit(readWhileOtherJarsAreClosed(churnFinished, ready, start, failure, expectedEntryCount)));
		}
		ready.await();
		start.countDown();
		try {
			awaitAll(futures);
		}
		finally {
			executor.shutdownNow();
		}
		if (failure.get() != null) {
			throw failure.get();
		}
		readEverything(this.jarFile, expectedEntryCount);
	}

	private Callable<Void> openAndCloseOtherJars(File file, CountDownLatch ready, CountDownLatch start,
			CountDownLatch churnFinished) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			try {
				for (int i = 0; i < SHARED_ZIP_CONTENT_CHURN_ITERATIONS; i++) {
					new NestedJarFile(file).close();
				}
			}
			finally {
				churnFinished.countDown();
			}
			return null;
		};
	}

	private Callable<Void> readWhileOtherJarsAreClosed(CountDownLatch churnFinished, CountDownLatch ready,
			CountDownLatch start, AtomicReference<AssertionError> failure, int expectedEntryCount) {
		return () -> {
			ready.countDown();
			awaitUninterruptibly(start);
			while (churnFinished.getCount() > 0 && failure.get() == null) {
				try {
					readEverything(this.jarFile, expectedEntryCount);
				}
				catch (Throwable ex) {
					failure.compareAndSet(null,
							new AssertionError("Closing another jar on the same file broke this one: " + ex, ex));
					return null;
				}
			}
			return null;
		};
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

	private static void spin(int iterations) {
		for (int i = 0; i < iterations; i++) {
			Thread.onSpinWait();
		}
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
