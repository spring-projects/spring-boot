/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.util.Assert;

/**
 * A snapshot of a directory at a given point in time.
 *
 * @author Phillip Webb
 */
class DirectorySnapshot {

	private final File directory;

	private final Date time;

	private final Set<FileSnapshot> files;

	/**
	 * Create a new {@link DirectorySnapshot} for the given directory.
	 * @param directory the source directory
	 */
	DirectorySnapshot(File directory) {
		Assert.notNull(directory, "'directory' must not be null");
		Assert.isTrue(!directory.isFile(), () -> "'directory' [%s] must not be a file".formatted(directory));
		this.directory = directory;
		this.time = new Date();
		Set<FileSnapshot> files = new LinkedHashSet<>();
		collectFiles(directory.toPath(), false, files);
		this.files = Collections.unmodifiableSet(files);
	}

	private static void collectFiles(Path source, boolean followLinks, Set<FileSnapshot> result) {
		Set<FileVisitOption> options = (followLinks) ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
				: EnumSet.noneOf(FileVisitOption.class);
		try {
			Files.walkFileTree(source, options, Integer.MAX_VALUE, new FileSnapshotCollector(followLinks, result));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	ChangedFiles getChangedFiles(DirectorySnapshot snapshot, @Nullable FileFilter triggerFilter) {
		Assert.notNull(snapshot, "'snapshot' must not be null");
		File directory = this.directory;
		Assert.isTrue(snapshot.directory.equals(directory),
				() -> "'snapshot' source directory must be '" + directory + "'");
		Set<ChangedFile> changes = new LinkedHashSet<>();
		Map<File, FileSnapshot> previousFiles = getFilesMap();
		for (FileSnapshot currentFile : snapshot.files) {
			if (acceptChangedFile(triggerFilter, currentFile)) {
				FileSnapshot previousFile = previousFiles.remove(currentFile.getFile());
				if (previousFile == null) {
					changes.add(new ChangedFile(directory, currentFile.getFile(), Type.ADD));
				}
				else if (!previousFile.equals(currentFile)) {
					changes.add(new ChangedFile(directory, currentFile.getFile(), Type.MODIFY));
				}
			}
		}
		for (FileSnapshot previousFile : previousFiles.values()) {
			if (acceptChangedFile(triggerFilter, previousFile)) {
				changes.add(new ChangedFile(directory, previousFile.getFile(), Type.DELETE));
			}
		}
		return new ChangedFiles(directory, changes);
	}

	private boolean acceptChangedFile(@Nullable FileFilter triggerFilter, FileSnapshot file) {
		return (triggerFilter == null || !triggerFilter.accept(file.getFile()));
	}

	private Map<File, FileSnapshot> getFilesMap() {
		Map<File, FileSnapshot> files = new LinkedHashMap<>();
		for (FileSnapshot file : this.files) {
			files.put(file.getFile(), file);
		}
		return files;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof DirectorySnapshot other) {
			return equals(other, null);
		}
		return super.equals(obj);
	}

	boolean equals(DirectorySnapshot other, @Nullable FileFilter filter) {
		if (this.directory.equals(other.directory)) {
			Set<FileSnapshot> ourFiles = filter(this.files, filter);
			Set<FileSnapshot> otherFiles = filter(other.files, filter);
			return ourFiles.equals(otherFiles);
		}
		return false;
	}

	private Set<FileSnapshot> filter(Set<FileSnapshot> source, @Nullable FileFilter filter) {
		if (filter == null) {
			return source;
		}
		Set<FileSnapshot> filtered = new LinkedHashSet<>();
		for (FileSnapshot file : source) {
			if (filter.accept(file.getFile())) {
				filtered.add(file);
			}
		}
		return filtered;
	}

	@Override
	public int hashCode() {
		int hashCode = this.directory.hashCode();
		hashCode = 31 * hashCode + this.files.hashCode();
		return hashCode;
	}

	/**
	 * Return the source directory of this snapshot.
	 * @return the source directory
	 */
	File getDirectory() {
		return this.directory;
	}

	@Override
	public String toString() {
		return this.directory + " snapshot at " + this.time;
	}

	/**
	 * {@link SimpleFileVisitor} that collects a {@link FileSnapshot} for each file. The
	 * attributes of each file are those returned when its directory is listed so that the
	 * file system is not queried again for every file. Symbolic links are followed.
	 */
	private static final class FileSnapshotCollector extends SimpleFileVisitor<Path> {

		private final boolean followLinks;

		private final Set<FileSnapshot> result;

		FileSnapshotCollector(boolean followLinks, Set<FileSnapshot> result) {
			this.followLinks = followLinks;
			this.result = result;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
			if (attributes.isSymbolicLink()) {
				if (!this.followLinks) {
					collectFiles(file, true, this.result);
				}
			}
			else if (attributes.isRegularFile() || file.toFile().isFile()) {
				this.result.add(new FileSnapshot(file.toFile(), attributes));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException ex) {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException ex) {
			return FileVisitResult.CONTINUE;
		}

	}

}
