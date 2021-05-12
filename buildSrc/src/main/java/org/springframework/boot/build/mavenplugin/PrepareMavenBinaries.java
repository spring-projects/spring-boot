/*
 * Copyright 2019-2020 the original author or authors.
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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} to make Maven binaries available for integration testing.
 *
 * @author Andy Wilkinson
 */
public class PrepareMavenBinaries extends DefaultTask {

	private Set<String> versions = new LinkedHashSet<>();

	private File outputDir;

	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@Input
	public Set<String> getVersions() {
		return this.versions;
	}

	public void versions(String... versions) {
		this.versions.addAll(Arrays.asList(versions));
	}

	@TaskAction
	public void prepareBinaries() {
		for (String version : this.versions) {
			Configuration configuration = getProject().getConfigurations().detachedConfiguration(
					getProject().getDependencies().create("org.apache.maven:apache-maven:" + version + ":bin@zip"));
			getProject().copy(
					(copy) -> copy.into(this.outputDir).from(getProject().zipTree(configuration.getSingleFile())));
		}
	}

}
