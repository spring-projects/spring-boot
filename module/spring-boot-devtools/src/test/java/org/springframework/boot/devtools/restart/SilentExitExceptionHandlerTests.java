/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.util.concurrent.CountDownLatch;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link SilentExitExceptionHandler}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SilentExitExceptionHandlerTests {

	@Test
	void setupAndExit() throws Exception {
		TestThread testThread = new TestThread() {
			@Override
			public void run() {
				SilentExitExceptionHandler.exitCurrentThread();
				fail("Didn't exit");
			}
		};
		SilentExitExceptionHandler.setup(testThread);
		testThread.startAndJoin();
		assertThat(testThread.getThrown()).isNull();
	}

	@Test
	void doesNotInterfereWithOtherExceptions() throws Exception {
		TestThread testThread = new TestThread() {
			@Override
			public void run() {
				throw new IllegalStateException("Expected");
			}
		};
		SilentExitExceptionHandler.setup(testThread);
		testThread.startAndJoin();
		Throwable thrown = testThread.getThrown();
		assertThat(thrown).isNotNull();
		assertThat(thrown.getMessage()).isEqualTo("Expected");
	}

	@Test
	void preventsNonZeroExitCodeWhenAllOtherThreadsAreDaemonThreads() {
		try {
			SilentExitExceptionHandler.exitCurrentThread();
		}
		catch (Exception ex) {
			TestSilentExitExceptionHandler silentExitExceptionHandler = new TestSilentExitExceptionHandler();
			silentExitExceptionHandler.uncaughtException(Thread.currentThread(), ex);
			try {
				assertThat(silentExitExceptionHandler.nonZeroExitCodePrevented).isTrue();
			}
			finally {
				silentExitExceptionHandler.cleanUp();
			}
		}

	}

	static class TestThread extends Thread {

		private @Nullable Throwable thrown;

		TestThread() {
			setUncaughtExceptionHandler((thread, exception) -> TestThread.this.thrown = exception);
		}

		@Nullable Throwable getThrown() {
			return this.thrown;
		}

		void startAndJoin() throws InterruptedException {
			start();
			join();
		}

	}

	static class TestSilentExitExceptionHandler extends SilentExitExceptionHandler {

		private boolean nonZeroExitCodePrevented;

		private final Object monitor = new Object();

		TestSilentExitExceptionHandler() {
			super(null);
		}

		@Override
		protected void preventNonZeroExitCode() {
			this.nonZeroExitCodePrevented = true;
		}

		@Override
		protected Thread[] getAllThreads() {
			final CountDownLatch threadRunning = new CountDownLatch(1);
			Thread daemonThread = new Thread(() -> {
				synchronized (TestSilentExitExceptionHandler.this.monitor) {
					threadRunning.countDown();
					try {
						TestSilentExitExceptionHandler.this.monitor.wait();
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			});
			daemonThread.setDaemon(true);
			daemonThread.start();
			try {
				threadRunning.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new Thread[] { Thread.currentThread(), daemonThread };
		}

		private void cleanUp() {
			synchronized (this.monitor) {
				this.monitor.notifyAll();
			}
		}

	}

}
