/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

import org.springframework.boot.build.context.properties.ConfigurationPropertiesAnalyzer.Report;

/**
 * {@link SourceTask} that checks {@code spring-configuration-metadata.json} files.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckSpringConfigurationMetadata extends DefaultTask {

	private final File projectRoot;

	public CheckSpringConfigurationMetadata() {
		this.projectRoot = getProject().getProjectDir();
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getMetadataLocation();

	@Input
	public abstract ListProperty<String> getExclusions();

	@TaskAction
	void check() throws IOException {
		Report report = new Report(this.projectRoot);
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(
				List.of(getMetadataLocation().get().getAsFile()));
		analyzer.analyzePropertyDescription(report, getExclusions().get());
		File reportFile = getReportLocation().get().getAsFile();
		report.write(reportFile);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in Spring configuration metadata. See " + reportFile + " for details.");
		}
	}

}
