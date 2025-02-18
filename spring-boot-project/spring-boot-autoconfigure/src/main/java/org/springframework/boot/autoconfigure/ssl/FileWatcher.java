/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * Watches files and directories and triggers a callback on change.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class FileWatcher implements Closeable {

	private static final Log logger = LogFactory.getLog(FileWatcher.class);

	private final Duration quietPeriod;

	private final Object lock = new Object();

	private WatcherThread thread;

	/**
	 * Create a new {@link FileWatcher} instance.
	 * @param quietPeriod the duration that no file changes should occur before triggering
	 * actions
	 */
	FileWatcher(Duration quietPeriod) {
		Assert.notNull(quietPeriod, "'quietPeriod' must not be null");
		this.quietPeriod = quietPeriod;
	}

	/**
	 * Watch the given files or directories for changes.
	 * @param paths the files or directories to watch
	 * @param action the action to take when changes are detected
	 */
	void watch(Set<Path> paths, Runnable action) {
		Assert.notNull(paths, "'paths' must not be null");
		Assert.notNull(action, "'action' must not be null");
		if (paths.isEmpty()) {
			return;
		}
		synchronized (this.lock) {
			try {
				if (this.thread == null) {
					this.thread = new WatcherThread();
					this.thread.start();
				}
				Set<Path> actualPaths = new HashSet<>();
				for (Path path : paths) {
					actualPaths.add(resolveSymlinkIfNecessary(path));
				}
				this.thread.register(new Registration(actualPaths, action));
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to register paths for watching: " + paths, ex);
			}
		}
	}

	private static Path resolveSymlinkIfNecessary(Path path) throws IOException {
		if (Files.isSymbolicLink(path)) {
			Path target = path.resolveSibling(Files.readSymbolicLink(path));
			return resolveSymlinkIfNecessary(target);
		}
		return path;
	}

	@Override
	public void close() throws IOException {
		synchronized (this.lock) {
			if (this.thread != null) {
				this.thread.close();
				this.thread.interrupt();
				try {
					this.thread.join();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				this.thread = null;
			}
		}
	}

	/**
	 * The watcher thread used to check for changes.
	 */
	private class WatcherThread extends Thread implements Closeable {

		private final WatchService watchService = FileSystems.getDefault().newWatchService();

		private final Map<WatchKey, List<Registration>> registrations = new ConcurrentHashMap<>();

		private volatile boolean running = true;

		WatcherThread() throws IOException {
			setName("ssl-bundle-watcher");
			setDaemon(true);
			setUncaughtExceptionHandler(this::onThreadException);
		}

		private void onThreadException(Thread thread, Throwable throwable) {
			logger.error("Uncaught exception in file watcher thread", throwable);
		}

		void register(Registration registration) throws IOException {
			for (Path path : registration.paths()) {
				if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
					throw new IOException("'%s' is neither a file nor a directory".formatted(path));
				}
				Path directory = Files.isDirectory(path) ? path : path.getParent();
				WatchKey watchKey = register(directory);
				this.registrations.computeIfAbsent(watchKey, (key) -> new CopyOnWriteArrayList<>()).add(registration);
			}
		}

		private WatchKey register(Path directory) throws IOException {
			logger.debug(LogMessage.format("Registering '%s'", directory));
			return directory.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
		}

		@Override
		public void run() {
			logger.debug("Watch thread started");
			Set<Runnable> actions = new HashSet<>();
			while (this.running) {
				try {
					long timeout = FileWatcher.this.quietPeriod.toMillis();
					WatchKey key = this.watchService.poll(timeout, TimeUnit.MILLISECONDS);
					if (key == null) {
						actions.forEach(this::runSafely);
						actions.clear();
					}
					else {
						accumulate(key, actions);
						key.reset();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				catch (ClosedWatchServiceException ex) {
					logger.debug("File watcher has been closed");
					this.running = false;
				}
			}
			logger.debug("Watch thread stopped");
		}

		private void runSafely(Runnable action) {
			try {
				action.run();
			}
			catch (Throwable ex) {
				logger.error("Unexpected SSL reload error", ex);
			}
		}

		private void accumulate(WatchKey key, Set<Runnable> actions) {
			List<Registration> registrations = this.registrations.get(key);
			Path directory = (Path) key.watchable();
			for (WatchEvent<?> event : key.pollEvents()) {
				Path file = directory.resolve((Path) event.context());
				for (Registration registration : registrations) {
					if (registration.manages(file)) {
						actions.add(registration.action());
					}
				}
			}
		}

		@Override
		public void close() throws IOException {
			this.running = false;
			this.watchService.close();
		}

	}

	/**
	 * An individual watch registration.
	 */
	private record Registration(Set<Path> paths, Runnable action) {

		Registration {
			paths = paths.stream().map(Path::toAbsolutePath).collect(Collectors.toSet());
		}

		boolean manages(Path file) {
			Path absolutePath = file.toAbsolutePath();
			return this.paths.contains(absolutePath) || isInDirectories(absolutePath);
		}

		private boolean isInDirectories(Path file) {
			return this.paths.stream().filter(Files::isDirectory).anyMatch(file::startsWith);
		}
	}

}
