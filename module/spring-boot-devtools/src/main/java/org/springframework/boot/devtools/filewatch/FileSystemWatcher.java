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

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.FileFilter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Watches specific directories for file changes.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @author Kamill Krzywanski
 * @since 1.3.0
 * @see FileChangeListener
 */
public class FileSystemWatcher {

	private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(1000);

	private static final Duration DEFAULT_QUIET_PERIOD = Duration.ofMillis(400);

	private final List<FileChangeListener> listeners = new ArrayList<>();

	private final boolean daemon;

	private final long pollInterval;

	private final long quietPeriod;

	private final SnapshotStateRepository snapshotStateRepository;

	private final AtomicInteger remainingScans = new AtomicInteger(-1);

	private final Map<File, @Nullable DirectorySnapshot> directories = new HashMap<>();

	private @Nullable FileFilter triggerFilter;

	private final Object monitor = new Object();

	private @Nullable ScheduledFuture<?> scheduledTask;

	/**
	 * Create a new {@link FileSystemWatcher} instance.
	 */
	public FileSystemWatcher() {
		this(true, DEFAULT_POLL_INTERVAL, DEFAULT_QUIET_PERIOD);
	}

	/**
	 * Create a new {@link FileSystemWatcher} instance.
	 * @param daemon if a daemon thread used to monitor changes
	 * @param pollInterval the amount of time to wait between checking for changes
	 * @param quietPeriod the amount of time required after a change has been detected to
	 * ensure that updates have completed
	 */
	public FileSystemWatcher(boolean daemon, Duration pollInterval, Duration quietPeriod) {
		this(daemon, pollInterval, quietPeriod, null);
	}

	/**
	 * Create a new {@link FileSystemWatcher} instance.
	 * @param daemon if a daemon thread used to monitor changes
	 * @param pollInterval the amount of time to wait between checking for changes
	 * @param quietPeriod the amount of time required after a change has been detected to
	 * ensure that updates have completed
	 * @param snapshotStateRepository the snapshot state repository
	 * @since 2.4.0
	 */
	public FileSystemWatcher(boolean daemon, Duration pollInterval, Duration quietPeriod,
			@Nullable SnapshotStateRepository snapshotStateRepository) {
		Assert.notNull(pollInterval, "'pollInterval' must not be null");
		Assert.notNull(quietPeriod, "'quietPeriod' must not be null");
		Assert.isTrue(pollInterval.toMillis() > 0, "'pollInterval' must be positive");
		Assert.isTrue(quietPeriod.toMillis() > 0, "'quietPeriod' must be positive");
		Assert.isTrue(pollInterval.toMillis() > quietPeriod.toMillis(),
				"'pollInterval' must be greater than QuietPeriod");
		this.daemon = daemon;
		this.pollInterval = pollInterval.toMillis();
		this.quietPeriod = quietPeriod.toMillis();
		this.snapshotStateRepository = (snapshotStateRepository != null) ? snapshotStateRepository
				: SnapshotStateRepository.NONE;
	}

	/**
	 * Add listener for file change events. Cannot be called after the watcher has been
	 * {@link #start() started}.
	 * @param fileChangeListener the listener to add
	 */
	public void addListener(FileChangeListener fileChangeListener) {
		Assert.notNull(fileChangeListener, "'fileChangeListener' must not be null");
		synchronized (this.monitor) {
			checkNotStarted();
			this.listeners.add(fileChangeListener);
		}
	}

	/**
	 * Add source directories to monitor. Cannot be called after the watcher has been
	 * {@link #start() started}.
	 * @param directories the directories to monitor
	 */
	public void addSourceDirectories(Iterable<File> directories) {
		Assert.notNull(directories, "'directories' must not be null");
		synchronized (this.monitor) {
			directories.forEach(this::addSourceDirectory);
		}
	}

	/**
	 * Add a source directory to monitor. Cannot be called after the watcher has been
	 * {@link #start() started}.
	 * @param directory the directory to monitor
	 */
	public void addSourceDirectory(File directory) {
		Assert.notNull(directory, "'directory' must not be null");
		Assert.isTrue(!directory.isFile(), () -> "'directory' [%s] must not be a file".formatted(directory));
		synchronized (this.monitor) {
			checkNotStarted();
			this.directories.put(directory, null);
		}
	}

	/**
	 * Set an optional {@link FileFilter} used to limit the files that trigger a change.
	 * @param triggerFilter a trigger filter or null
	 */
	public void setTriggerFilter(@Nullable FileFilter triggerFilter) {
		synchronized (this.monitor) {
			this.triggerFilter = triggerFilter;
		}
	}

	private void checkNotStarted() {
		Assert.state(this.scheduledTask == null, "FileSystemWatcher already started");
	}

	/**
	 * Start monitoring the source directory for changes.
	 */
	public void start() {
		synchronized (this.monitor) {
			createOrRestoreInitialSnapshots();
			if (this.scheduledTask == null) {
				Map<File, DirectorySnapshot> localDirectories = new HashMap<>(this.directories);
				var watchThread = new Watcher(this.remainingScans, new ArrayList<>(this.listeners), this.triggerFilter,
						this.pollInterval, this.quietPeriod, localDirectories, this.snapshotStateRepository,
						this.daemon);
				this.scheduledTask = watchThread.getScheduledTask();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void createOrRestoreInitialSnapshots() {
		Map<File, DirectorySnapshot> restored = (Map<File, DirectorySnapshot>) this.snapshotStateRepository.restore();
		this.directories.replaceAll((f, v) -> {
			DirectorySnapshot restoredSnapshot = (restored != null) ? restored.get(f) : null;
			return (restoredSnapshot != null) ? restoredSnapshot : new DirectorySnapshot(f);
		});
	}

	/**
	 * Stop monitoring the source directories.
	 */
	public void stop() {
		stopAfter(0);
	}

	/**
	 * Stop monitoring the source directories.
	 * @param remainingScans the number of remaining scans
	 */
	void stopAfter(int remainingScans) {
		ScheduledFuture<?> scheduledFuture;
		synchronized (this.monitor) {
			scheduledFuture = this.scheduledTask;
			if (scheduledFuture != null) {
				this.remainingScans.set(remainingScans);
				if (remainingScans <= 0) {
					scheduledFuture.cancel(true);
				}
			}
		}

		if (scheduledFuture != null) {
			try {
				scheduledFuture.get();
			}
			catch (CancellationException ignored) {
			}
			catch (InterruptedException | ExecutionException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static final class Watcher extends ScheduledThreadPoolExecutor {

		private final AtomicInteger remainingScans;

		private final List<FileChangeListener> listeners;

		private final @Nullable FileFilter triggerFilter;

		private final long pollInterval;

		private final long quietPeriod;

		private Map<File, DirectorySnapshot> directories;

		private final ScheduledFuture<?> scheduledTask;

		private final SnapshotStateRepository snapshotStateRepository;

		private Watcher(AtomicInteger remainingScans, List<FileChangeListener> listeners,
				@Nullable FileFilter triggerFilter, long pollInterval, long quietPeriod,
				Map<File, DirectorySnapshot> directories, SnapshotStateRepository snapshotStateRepository,
				boolean daemon) {
			super(1, (r) -> {
				Thread t = new Thread(r, "FileSystemWatcher");
				t.setDaemon(daemon);
				return t;
			});
			this.remainingScans = remainingScans;
			this.listeners = listeners;
			this.triggerFilter = triggerFilter;
			this.pollInterval = pollInterval;
			this.quietPeriod = quietPeriod;
			this.directories = directories;
			this.snapshotStateRepository = snapshotStateRepository;
			this.scheduledTask = startInitialScan();
		}

		private ScheduledFuture<?> startInitialScan() {
			return this.scheduleWithFixedDelay(() -> {
				if (this.remainingScans.get() == 0) {
					shutdown();
					return;
				}
				if (this.remainingScans.get() > 0) {
					this.remainingScans.decrementAndGet();
				}
				scheduleScan(this.directories);
			}, 0, this.pollInterval, TimeUnit.MILLISECONDS);
		}

		private void scheduleScan(Map<File, DirectorySnapshot> previousSnapshots) {
			Map<File, DirectorySnapshot> currentSnapshots = getCurrentSnapshots();
			if (isDifferent(previousSnapshots, currentSnapshots)) {
				this.schedule(() -> scheduleScan(currentSnapshots), this.quietPeriod, TimeUnit.MILLISECONDS);
			}
			else if (isDifferent(this.directories, currentSnapshots)) {
				updateSnapshots(currentSnapshots.values());
			}
		}

		private boolean isDifferent(Map<File, DirectorySnapshot> previous, Map<File, DirectorySnapshot> current) {
			if (!previous.keySet().equals(current.keySet())) {
				return true;
			}
			for (Map.Entry<File, DirectorySnapshot> entry : previous.entrySet()) {
				DirectorySnapshot previousDirectory = entry.getValue();
				DirectorySnapshot currentDirectory = current.get(entry.getKey());
				Assert.state(currentDirectory != null, "'currentDirectory' must not be null");
				if (!previousDirectory.equals(currentDirectory, this.triggerFilter)) {
					return true;
				}
			}
			return false;
		}

		private Map<File, DirectorySnapshot> getCurrentSnapshots() {
			Map<File, DirectorySnapshot> snapshots = new LinkedHashMap<>();
			for (File directory : this.directories.keySet()) {
				snapshots.put(directory, new DirectorySnapshot(directory));
			}
			return snapshots;
		}

		private void updateSnapshots(Collection<DirectorySnapshot> snapshots) {
			Map<File, DirectorySnapshot> updated = new LinkedHashMap<>();
			Set<ChangedFiles> changeSet = new LinkedHashSet<>();
			for (DirectorySnapshot snapshot : snapshots) {
				DirectorySnapshot previous = this.directories.get(snapshot.getDirectory());
				updated.put(snapshot.getDirectory(), snapshot);
				Assert.state(previous != null, "'previous' must not be null");
				ChangedFiles changedFiles = previous.getChangedFiles(snapshot, this.triggerFilter);
				if (!changedFiles.getFiles().isEmpty()) {
					changeSet.add(changedFiles);
				}
			}
			this.directories = updated;
			this.snapshotStateRepository.save(updated);
			if (!changeSet.isEmpty()) {
				fireListeners(Collections.unmodifiableSet(changeSet));
			}
		}

		private void fireListeners(Set<ChangedFiles> changeSet) {
			for (FileChangeListener listener : this.listeners) {
				listener.onChange(changeSet);
			}
		}

		private ScheduledFuture<?> getScheduledTask() {
			return this.scheduledTask;
		}

	}

}
