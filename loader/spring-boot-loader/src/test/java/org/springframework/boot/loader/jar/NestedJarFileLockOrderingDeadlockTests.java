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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.testsupport.TestJar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces a deadlock caused by {@link NestedJarFile} locking on
 * {@code synchronized (this)}. One thread can hold an unrelated lock (e.g., ClassLoader or
 * reflection machinery) while waiting on {@code NestedJarFile}'s monitor, while another
 * thread holds that monitor while waiting on the unrelated lock.
 *
 * <p>
 * This reflects real-world scenarios from gh-51463 and gh-51379 where concurrent jar
 * operations under class-loading/reflection contention trigger the deadlock. Two
 * {@link CountDownLatch}es force each thread into "holding one lock, about to request
 * the other" before either attempts the second lock, making the deadlock easily
 * reproducible. The test fails (times out) against the {@code synchronized (this)}
 * implementation, and passes once {@code NestedJarFile} stops exposing its monitor.
 *
 * <p>
 * The thread dumps in gh-51463 and gh-51379 do not show the thread as being deadlocked,
 * which is due to the difference in how the JVM's own deadlock detector handles standard
 * vs virtual threads.  Both gh-51463 and gh-51379 specify explicitly virtual threads are
 * part of their scenarios. Ideally, this test would be updated to use virtual threads but
 * current language level standards prevent the test from using virtual threads
 * (requires JDK21 level).
 *
 * <p>
 * The default timeout is 5 seconds (for CI), but can be overridden by setting
 * {@code test.deadlock.hang=true} to block forever (for capturing thread dumps). When a thread
 * dump is captured:
 * <pre>
 * jcmd &lt;pid&gt; Thread.print > thread-dump.txt
 * jcmd &lt;pid&gt; Thread.dump_to_file -format=json thread-dump.json
 * </pre>
 * the result is different when virtual threads are used vs standard threads. Standard threads
 * will show as a deadlock in the text dump but when virtual threads are deadlocked they just
 * show as blocked in the json dump.
 *
 * @author Ian Kettle
 */
class NestedJarFileLockOrderingDeadlockTests {

	@TempDir
	File tempDir;

	@Test
	void forceContentionForLock() throws Exception {
		File jarFile = new File(this.tempDir, "test.jar");
		TestJar.create(jarFile);
		NestedJarFile jar = new NestedJarFile(jarFile);

		Object externalLock = new Object();
		CountDownLatch thread1HoldsExternalLock = new CountDownLatch(1);
		CountDownLatch thread2HoldsJarMonitor = new CountDownLatch(1);
		CountDownLatch bothThreadsFinished = new CountDownLatch(2);

		Thread holdsExternalLockThenWantsJarMonitor = new Thread(() -> {
			synchronized (externalLock) {
				thread1HoldsExternalLock.countDown();
				awaitUninterruptibly(thread2HoldsJarMonitor);
				jar.hasEntry("1.dat");
			}
			bothThreadsFinished.countDown();
		}, "holds-external-lock-then-wants-jar-monitor");
		holdsExternalLockThenWantsJarMonitor.setDaemon(true);

		// Only possible because NestedJarFile exposes its monitor via
		// synchronized (this); external code can acquire it directly.
		Thread holdsJarMonitorThenWantsExternalLock = new Thread(() -> {
			synchronized (jar) {
				thread2HoldsJarMonitor.countDown();
				awaitUninterruptibly(thread1HoldsExternalLock);
				// Try to acquire external lock while holding jar monitor.
				// Thread 1 already holds this lock, so we block here → deadlock.
				synchronized (externalLock) {
					jar.size();
				}
			}
			bothThreadsFinished.countDown();
		}, "holds-jar-monitor-then-wants-external-lock");
		holdsJarMonitorThenWantsExternalLock.setDaemon(true);

		holdsExternalLockThenWantsJarMonitor.start();
		holdsJarMonitorThenWantsExternalLock.start();

		boolean completed = waitForCompletion(bothThreadsFinished);

		// A deadlock leaves jar's monitor held forever, so jar.close() would also hang.
		if (completed) {
			jar.close();
		}

		assertThat(completed)
			.as("NestedJarFile's synchronized (this) monitor is exposed to external code, "
					+ "allowing a lock-order-inversion deadlock")
			.isTrue();
	}

	private boolean waitForCompletion(CountDownLatch latch) throws InterruptedException {
		// Set 'test.deadlock.hang=true' to block forever (for capturing thread dumps).
		// Default: timeout after 5 seconds (for CI).
		boolean hang = Boolean.parseBoolean(System.getProperty("test.deadlock.hang", "false"));
		if (hang) {
			latch.await();
			return true;
		}
		return latch.await(5, TimeUnit.SECONDS);
	}

	private static void awaitUninterruptibly(CountDownLatch latch) {
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
