/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build.starters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.springframework.core.CollectionFactory;

/**
 * A {@link Task} for generating metadata that describes a starter.
 *
 * @author Andy Wilkinson
 */
public class StarterMetadata extends AbstractTask {

	private Configuration dependencies;

	private File destination;

	public StarterMetadata() {
		getInputs().property("name", (Callable<String>) () -> getProject().getName());
		getInputs().property("description", (Callable<String>) () -> getProject().getDescription());
	}

	@Classpath
	public FileCollection getDependencies() {
		return this.dependencies;
	}

	public void setDependencies(Configuration dependencies) {
		this.dependencies = dependencies;
	}

	@OutputFile
	public File getDestination() {
		return this.destination;
	}

	public void setDestination(File destination) {
		this.destination = destination;
	}

	@TaskAction
	void generateMetadata() throws IOException {
		Properties properties = CollectionFactory.createSortedProperties(true);
		properties.setProperty("name", getProject().getName());
		properties.setProperty("description", getProject().getDescription());
		properties.setProperty("dependencies", String.join(",", this.dependencies.getResolvedConfiguration()
				.getResolvedArtifacts().stream().map(ResolvedArtifact::getName).collect(Collectors.toSet())));
		this.destination.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(this.destination)) {
			properties.store(writer, null);
		}
	}

}
