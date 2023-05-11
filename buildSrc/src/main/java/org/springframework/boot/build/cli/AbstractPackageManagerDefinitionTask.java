/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskExecutionException;

import org.springframework.boot.build.artifacts.ArtifactRelease;

/**
 * Base class for generating a package manager definition file such as a Scoop manifest or
 * a Homebrew formula.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public abstract class AbstractPackageManagerDefinitionTask extends DefaultTask {

	private Provider<RegularFile> archive;

	private File template;

	private File outputDir;

	public AbstractPackageManagerDefinitionTask() {
		getInputs().property("version", getProject().provider(getProject()::getVersion));
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFile getArchive() {
		return this.archive.get();
	}

	public void setArchive(Provider<RegularFile> archive) {
		this.archive = archive;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public File getTemplate() {
		return this.template;
	}

	public void setTemplate(File template) {
		this.template = template;
	}

	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	protected void createDescriptor(Map<String, Object> additionalProperties) {
		getProject().copy((copy) -> {
			copy.from(this.template);
			copy.into(this.outputDir);
			copy.expand(getProperties(additionalProperties));
		});
	}

	private Map<String, Object> getProperties(Map<String, Object> additionalProperties) {
		Map<String, Object> properties = new HashMap<>(additionalProperties);
		Project project = getProject();
		properties.put("hash", sha256(this.archive.get().getAsFile()));
		properties.put("repo", ArtifactRelease.forProject(project).getDownloadRepo());
		properties.put("project", project);
		return properties;
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

}
