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

package org.springframework.boot.build.cli;

import java.io.File;
import java.security.MessageDigest;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.artifacts.ArtifactRelease;
import org.springframework.boot.build.properties.BuildProperties;
import org.springframework.boot.build.properties.BuildType;

/**
 * A {@link Task} for creating a Homebrew formula manifest.
 *
 * @author Andy Wilkinson
 */
public abstract class HomebrewFormula extends DefaultTask {

	private static final Logger logger = LoggerFactory.getLogger(HomebrewFormula.class);

	private final FileSystemOperations fileSystemOperations;

	private final BuildType buildType;

	@Inject
	public HomebrewFormula(FileSystemOperations fileSystemOperations) {
		this.fileSystemOperations = fileSystemOperations;
		Project project = getProject();
		MapProperty<String, Object> properties = getProperties();
		properties.put("hash", getArchive().map((archive) -> sha256(archive.getAsFile())));
		getProperties().put("repo", ArtifactRelease.forProject(project).getDownloadRepo());
		getProperties().put("version", project.getVersion().toString());
		this.buildType = BuildProperties.get(getProject()).buildType();
	}

	private String sha256(File file) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return new DigestUtils(digest).digestAsHex(file);
		}
		catch (Exception ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getArchive();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getTemplate();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDir();

	@Input
	abstract MapProperty<String, Object> getProperties();

	@TaskAction
	void createFormula() {
		if (this.buildType != BuildType.OPEN_SOURCE) {
			logger.debug("Skipping Homebrew formula for non open source build type");
			return;
		}
		this.fileSystemOperations.copy((copy) -> {
			copy.from(getTemplate());
			copy.into(getOutputDir());
			copy.expand(getProperties().get());
		});
	}

}
