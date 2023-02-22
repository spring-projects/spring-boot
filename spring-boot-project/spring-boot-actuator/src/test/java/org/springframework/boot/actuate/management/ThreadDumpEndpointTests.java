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

package org.springframework.boot.actuate.management;

import java.lang.Thread.State;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadDumpEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ThreadDumpEndpointTests {

	@Test
	void dumpThreads() {
		assertThat(new ThreadDumpEndpoint().threadDump().getThreads()).isNotEmpty();
	}

	@Test
	void dumpThreadsAsText() throws InterruptedException {
		Object contendedMonitor = new Object();
		Object monitor = new Object();
		CountDownLatch latch = new CountDownLatch(1);
		Thread awaitCountDownLatchThread = new Thread(() -> {
			try {
				latch.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}, "Awaiting CountDownLatch");
		awaitCountDownLatchThread.start();
		Thread contendedMonitorThread = new Thread(() -> {
			synchronized (contendedMonitor) {
				// Intentionally empty
			}
		}, "Waiting for monitor");
		Thread waitOnMonitorThread = new Thread(() -> {
			synchronized (monitor) {
				try {
					monitor.wait();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}, "Waiting on monitor");
		waitOnMonitorThread.start();
		ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		Lock writeLock = readWriteLock.writeLock();
		new Thread(() -> {
			writeLock.lock();
			try {
				latch.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			finally {
				writeLock.unlock();
			}
		}, "Holding write lock").start();
		while (writeLock.tryLock()) {
			writeLock.unlock();
		}
		awaitState(waitOnMonitorThread, State.WAITING);
		awaitState(awaitCountDownLatchThread, State.WAITING);
		String threadDump;
		synchronized (contendedMonitor) {
			contendedMonitorThread.start();
			awaitState(contendedMonitorThread, State.BLOCKED);
			threadDump = new ThreadDumpEndpoint().textThreadDump();
		}
		latch.countDown();
		synchronized (monitor) {
			monitor.notifyAll();
		}
		assertThat(threadDump)
			.containsPattern(String.format("\t- parking to wait for <[0-9a-z]+> \\(a %s\\$Sync\\)",
					CountDownLatch.class.getName().replace(".", "\\.")))
			.contains(String.format("\t- locked <%s> (a java.lang.Object)", hexIdentityHashCode(contendedMonitor)))
			.contains(String.format("\t- waiting to lock <%s> (a java.lang.Object) owned by \"%s\" t@%d",
					hexIdentityHashCode(contendedMonitor), Thread.currentThread().getName(),
					Thread.currentThread().getId()))
			.satisfiesAnyOf(
					(dump) -> assertThat(dump).contains(
							String.format("\t- waiting on <%s> (a java.lang.Object)", hexIdentityHashCode(monitor))),
					(dump) -> assertThat(dump).contains(String
						.format("\t- parking to wait for <%s> (a java.lang.Object)", hexIdentityHashCode(monitor))))
			.containsPattern(
					String.format("Locked ownable synchronizers:%n\t- Locked <[0-9a-z]+> \\(a %s\\$NonfairSync\\)",
							ReentrantReadWriteLock.class.getName().replace(".", "\\.")));
	}

	private String hexIdentityHashCode(Object object) {
		return Integer.toHexString(System.identityHashCode(object));
	}

	private void awaitState(Thread thread, State state) throws InterruptedException {
		while (thread.getState() != state) {
			Thread.sleep(50);
		}
	}

}
