/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.io.IOException;

import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

import org.springframework.boot.build.context.properties.ConfigurationPropertiesAnalyzer.Report;

/**
 * {@link SourceTask} that checks additional Spring configuration metadata files.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckAdditionalSpringConfigurationMetadata extends SourceTask {

	private final File projectDir;

	public CheckAdditionalSpringConfigurationMetadata() {
		this.projectDir = getProject().getProjectDir();
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@Override
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return super.getSource();
	}

	@TaskAction
	void check() throws IOException {
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(getSource().getFiles());
		Report report = new Report(this.projectDir);
		analyzer.analyzeOrder(report);
		analyzer.analyzeDuplicates(report);
		analyzer.analyzeDeprecationSince(report);
		File reportFile = getReportLocation().get().getAsFile();
		report.write(reportFile);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in additional Spring configuration metadata. See " + reportFile + " for details.");
		}
	}

}
