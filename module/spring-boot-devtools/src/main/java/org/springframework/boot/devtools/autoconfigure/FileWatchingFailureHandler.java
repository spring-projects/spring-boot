/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.devtools.classpath.ClassPathDirectories;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.boot.devtools.filewatch.FileSystemWatcherFactory;
import org.springframework.boot.devtools.restart.FailureHandler;
import org.springframework.boot.devtools.restart.Restarter;

/**
 * {@link FailureHandler} that waits for filesystem changes before retrying.
 *
 * @author Phillip Webb
 */
class FileWatchingFailureHandler implements FailureHandler {

	private final FileSystemWatcherFactory fileSystemWatcherFactory;

	FileWatchingFailureHandler(FileSystemWatcherFactory fileSystemWatcherFactory) {
		this.fileSystemWatcherFactory = fileSystemWatcherFactory;
	}

	@Override
	public Outcome handle(Throwable failure) {
		CountDownLatch latch = new CountDownLatch(1);
		FileSystemWatcher watcher = this.fileSystemWatcherFactory.getFileSystemWatcher();
		watcher.addSourceDirectories(new ClassPathDirectories(Restarter.getInstance().getInitialUrls()));
		watcher.addListener(new Listener(latch));
		watcher.start();
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return Outcome.RETRY;
	}

	private static class Listener implements FileChangeListener {

		private final CountDownLatch latch;

		Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void onChange(Set<ChangedFiles> changeSet) {
			this.latch.countDown();
		}

	}

}
