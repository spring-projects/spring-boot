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
