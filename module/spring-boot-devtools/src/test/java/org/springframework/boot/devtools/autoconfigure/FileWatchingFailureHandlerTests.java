/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.restart.FailureHandler.Outcome;
import org.springframework.boot.devtools.restart.MockRestarter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;

/**
 * Tests for {@link FileWatchingFailureHandler}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(MockRestarter.class)
class FileWatchingFailureHandlerTests {

	private final FileSystemWatcher watcher = mock(FileSystemWatcher.class);

	private final FileWatchingFailureHandler handler = new FileWatchingFailureHandler(() -> this.watcher);

	@Test
	void handleWhenFilesChangeRetriesAndStopsWatcher() {
		HandlerThread thread = HandlerThread.start(this.handler);
		FileChangeListener listener = awaitListener();
		assertThat(thread.isDone()).isFalse();
		listener.onChange(Collections.emptySet());
		thread.join();
		assertThat(thread.outcome.get()).isEqualTo(Outcome.RETRY);
		InOrder ordered = inOrder(this.watcher);
		then(this.watcher).should(ordered).start();
		then(this.watcher).should(ordered).stop();
	}

	@Test
	void handleWhenInterruptedRetriesAndStopsWatcher() {
		HandlerThread thread = HandlerThread.start(this.handler);
		awaitListener();
		thread.interrupt();
		thread.join();
		assertThat(thread.outcome.get()).isEqualTo(Outcome.RETRY);
		assertThat(thread.interrupted.get()).isTrue();
		then(this.watcher).should().stop();
	}

	private FileChangeListener awaitListener() {
		then(this.watcher).should(timeout(5000)).start();
		ArgumentCaptor<FileChangeListener> listener = ArgumentCaptor.forClass(FileChangeListener.class);
		then(this.watcher).should().addListener(listener.capture());
		return listener.getValue();
	}

	private static final class HandlerThread {

		private final AtomicReference<Outcome> outcome = new AtomicReference<>();

		private final AtomicBoolean interrupted = new AtomicBoolean();

		private final Thread thread;

		private HandlerThread(FileWatchingFailureHandler handler) {
			this.thread = new Thread(() -> {
				this.outcome.set(handler.handle(new IllegalStateException("Failed")));
				this.interrupted.set(Thread.currentThread().isInterrupted());
			});
		}

		static HandlerThread start(FileWatchingFailureHandler handler) {
			HandlerThread result = new HandlerThread(handler);
			result.thread.start();
			return result;
		}

		boolean isDone() {
			return !this.thread.isAlive();
		}

		void interrupt() {
			this.thread.interrupt();
		}

		void join() {
			Awaitility.await().atMost(Duration.ofSeconds(5)).until(this::isDone);
		}

	}

}
