/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;

/**
 * A {@link Task} that uses a project's auto-configuration imports.
 *
 * @author Andy Wilkinson
 */
public abstract class AutoConfigurationImportsTask extends DefaultTask {

	/**
	 * The path of the {@code AutoConfiguration.imports} file.
	 */
	public static final String IMPORTS_FILE = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	private FileCollection sourceFiles = getProject().getObjects().fileCollection();

	@InputFiles
	@SkipWhenEmpty
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return this.sourceFiles.getAsFileTree().matching((filter) -> filter.include(IMPORTS_FILE));
	}

	public void setSource(Object source) {
		this.sourceFiles = getProject().getObjects().fileCollection().from(source);
	}

	protected List<String> loadImports() {
		File importsFile = getSource().getSingleFile();
		try {
			return Files.readAllLines(importsFile.toPath());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
