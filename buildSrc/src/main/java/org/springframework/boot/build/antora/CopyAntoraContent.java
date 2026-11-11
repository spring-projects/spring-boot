/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Tasks to copy Antora content.
 *
 * @author Andy Wilkinson
 */
public abstract class CopyAntoraContent extends DefaultTask {

	private FileCollection source;

	@Inject
	public CopyAntoraContent() {
	}

	@InputFiles
	public FileCollection getSource() {
		return this.source;
	}

	public void setSource(FileCollection source) {
		this.source = source;
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	void copyAntoraContent() throws IllegalStateException, IOException {
		Path source = this.source.getSingleFile().toPath();
		Path target = getOutputFile().getAsFile().get().toPath();
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
	}

}
