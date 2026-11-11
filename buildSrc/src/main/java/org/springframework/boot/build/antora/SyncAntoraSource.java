/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * Task sync Antora source.
 *
 * @author Andy Wilkinson
 */
public abstract class SyncAntoraSource extends DefaultTask {

	private final FileSystemOperations fileSystemOperations;

	private final ArchiveOperations archiveOperations;

	private FileCollection source;

	@Inject
	public SyncAntoraSource(FileSystemOperations fileSystemOperations, ArchiveOperations archiveOperations) {
		this.fileSystemOperations = fileSystemOperations;
		this.archiveOperations = archiveOperations;
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@InputFiles
	public FileCollection getSource() {
		return this.source;
	}

	public void setSource(FileCollection source) {
		this.source = source;
	}

	@TaskAction
	void syncAntoraSource() {
		this.fileSystemOperations.sync(this::syncAntoraSource);
	}

	private void syncAntoraSource(CopySpec sync) {
		sync.into(getOutputDirectory());
		this.source.getFiles().forEach((file) -> sync.from(this.archiveOperations.zipTree(file)));
	}

}
