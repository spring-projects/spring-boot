/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.api.Task;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import org.springframework.boot.loader.tools.BuildPropertiesWriter;
import org.springframework.boot.loader.tools.BuildPropertiesWriter.ProjectDetails;

/**
 * {@link Task} for generating a {@code build-info.properties} file from a
 * {@code Project}. The {@link #setDestinationDir destination dir} and
 * {@link #setProjectArtifact project artifact} must be configured before execution.
 *
 * @author Andy Wilkinson
 */
public class BuildInfo extends ConventionTask {

	private File destinationDir;

	private String projectGroup = getProject().getGroup().toString();

	private String projectArtifact;

	private String projectVersion = getProject().getVersion().toString();

	private String projectName = getProject().getName();

	private Map<String, Object> additionalProperties = new HashMap<>();

	@TaskAction
	public void generateBuildProperties() {
		try {
			new BuildPropertiesWriter(
					new File(getDestinationDir(), "build-info.properties"))
							.writeBuildProperties(new ProjectDetails(this.projectGroup,
									getProjectArtifact(), this.projectVersion,
									this.projectName,
									coerceToStringValues(this.additionalProperties)));
		}
		catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	@Input
	public String getProjectGroup() {
		return this.projectGroup;
	}

	public void setProjectGroup(String projectGroup) {
		this.projectGroup = projectGroup;
	}

	@Input
	public String getProjectArtifact() {
		return this.projectArtifact;
	}

	public void setProjectArtifact(String projectArtifact) {
		this.projectArtifact = projectArtifact;
	}

	@Input
	public String getProjectVersion() {
		return this.projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	@Input
	public String getProjectName() {
		return this.projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@OutputDirectory
	public File getDestinationDir() {
		return this.destinationDir;
	}

	public void setDestinationDir(File destinationDir) {
		this.destinationDir = destinationDir;
	}

	@Input
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	public void setAdditionalProperties(Map<String, Object> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	private Map<String, String> coerceToStringValues(Map<String, Object> input) {
		Map<String, String> output = new HashMap<>();
		for (Entry<String, Object> entry : input.entrySet()) {
			output.put(entry.getKey(), entry.getValue().toString());
		}
		return output;
	}

}
