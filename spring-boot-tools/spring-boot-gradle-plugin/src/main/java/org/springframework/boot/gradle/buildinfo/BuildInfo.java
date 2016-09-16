/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.gradle.buildinfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.loader.tools.BuildPropertiesWriter;
import org.springframework.boot.loader.tools.BuildPropertiesWriter.ProjectDetails;

/**
 * {@link DefaultTask} for generating a {@code build-info.properties} file from a
 * {@code Project}.
 * <p>
 * By default, the {@code build-info.properties} file is generated in
 * project.buildDir/resources/main/META-INF.
 * </p>
 *
 * @author Andy Wilkinson
 */
public class BuildInfo extends DefaultTask {

	@OutputFile
	private File outputFile = getProject().file(new File(getProject().getBuildDir(),
			"resources/main/META-INF/build-info.properties"));

	@Input
	private String projectGroup = getProject().getGroup().toString();

	@Input
	private String projectArtifact = ((Jar) getProject().getTasks()
			.getByName(JavaPlugin.JAR_TASK_NAME)).getBaseName();

	@Input
	private String projectVersion = getProject().getVersion().toString();

	@Input
	private String projectName = getProject().getName();

	@Input
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@TaskAction
	public void generateBuildProperties() {
		try {
			new BuildPropertiesWriter(this.outputFile)
					.writeBuildProperties(new ProjectDetails(this.projectGroup,
							this.projectArtifact, this.projectVersion, this.projectName,
							coerceToStringValues(this.additionalProperties)));
		}
		catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	public String getProjectGroup() {
		return this.projectGroup;
	}

	public void setProjectGroup(String projectGroup) {
		this.projectGroup = projectGroup;
	}

	public String getProjectArtifact() {
		return this.projectArtifact;
	}

	public void setProjectArtifact(String projectArtifact) {
		this.projectArtifact = projectArtifact;
	}

	public String getProjectVersion() {
		return this.projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public File getOutputFile() {
		return this.outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	public void setAdditionalProperties(Map<String, Object> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	private Map<String, String> coerceToStringValues(Map<String, Object> input) {
		Map<String, String> output = new HashMap<String, String>();
		for (Entry<String, Object> entry : input.entrySet()) {
			output.put(entry.getKey(), entry.getValue().toString());
		}
		return output;
	}

}
