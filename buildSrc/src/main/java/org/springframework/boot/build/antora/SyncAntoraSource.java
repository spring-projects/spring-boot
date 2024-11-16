/*
 * Copyright 2012-2024 the original author or authors.
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
