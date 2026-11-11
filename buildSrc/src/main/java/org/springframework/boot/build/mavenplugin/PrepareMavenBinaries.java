/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.mavenplugin;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} to make Maven binaries available for integration testing.
 *
 * @author Andy Wilkinson
 */
public abstract class PrepareMavenBinaries extends DefaultTask {

	private final FileSystemOperations fileSystemOperations;

	private final Provider<Set<FileTree>> binaries;

	@Inject
	public PrepareMavenBinaries(FileSystemOperations fileSystemOperations, ArchiveOperations archiveOperations) {
		this.fileSystemOperations = fileSystemOperations;
		ConfigurationContainer configurations = getProject().getConfigurations();
		DependencyHandler dependencies = getProject().getDependencies();
		this.binaries = getVersions().map((versions) -> versions.stream()
			.map((version) -> configurations
				.detachedConfiguration(dependencies.create("org.apache.maven:apache-maven:" + version + ":bin@zip")))
			.map(Configuration::getSingleFile)
			.map(archiveOperations::zipTree)
			.collect(Collectors.toSet()));
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDir();

	@Input
	public abstract SetProperty<String> getVersions();

	@TaskAction
	public void prepareBinaries() {
		this.fileSystemOperations.sync((sync) -> {
			sync.into(getOutputDir());
			this.binaries.get().forEach(sync::from);
		});
	}

}
