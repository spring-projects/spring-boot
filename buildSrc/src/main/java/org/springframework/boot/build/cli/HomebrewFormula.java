/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import org.springframework.boot.build.artifactory.ArtifactoryRepository;

/**
 * A {@link Task} for creating a Homebrew formula manifest.
 *
 * @author Andy Wilkinson
 */
public class HomebrewFormula extends DefaultTask {

	private static final String SPRING_REPO = "https://repo.spring.io/%s";

	private static final String MAVEN_REPO = "https://repo1.maven.org/maven2";

	private Provider<RegularFile> archive;

	private File template;

	private File outputDir;

	public HomebrewFormula() {
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
		properties.put("repo", getRepo(project));
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

	private String getRepo(Project project) {
		ArtifactoryRepository artifactoryRepo = ArtifactoryRepository.forProject(project);
		return (!artifactoryRepo.isRelease()) ? String.format(SPRING_REPO, artifactoryRepo.getName()) : MAVEN_REPO;
	}

	@TaskAction
	void createFormula() {
		createDescriptor(Collections.emptyMap());
	}

}
