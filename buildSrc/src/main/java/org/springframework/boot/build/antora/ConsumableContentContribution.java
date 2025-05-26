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

package org.springframework.boot.build.antora;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * A contribution of content to Antora that can be consumed by other projects.
 *
 * @author Andy Wilkinson
 */
class ConsumableContentContribution extends ContentContribution {

	protected ConsumableContentContribution(Project project, String type, String name) {
		super(project, name, type);
	}

	@Override
	void produceFrom(CopySpec copySpec) {
		this.produceFrom(copySpec, false);
	}

	void produceFrom(CopySpec copySpec, boolean publish) {
		TaskProvider<? extends Task> producer = super.configureProduction(copySpec);
		if (publish) {
			publish(producer);
		}
		Configuration configuration = createConfiguration(getName(),
				"Configuration for %s Antora %s content artifacts.");
		configuration.setCanBeConsumed(true);
		configuration.setCanBeResolved(false);
		getProject().getArtifacts().add(configuration.getName(), producer);
	}

	void consumeFrom(String path) {
		Configuration configuration = createConfiguration(getName(), "Configuration for %s Antora %s content.");
		configuration.setCanBeConsumed(false);
		configuration.setCanBeResolved(true);
		DependencyHandler dependencies = getProject().getDependencies();
		dependencies.add(configuration.getName(),
				getProject().provider(() -> projectDependency(path, configuration.getName())));
		Provider<Directory> outputDirectory = outputDirectory("content", getName());
		TaskContainer tasks = getProject().getTasks();
		TaskProvider<?> copyAntoraContent = tasks.register(taskName("copy", "%s", configuration.getName()),
				CopyAntoraContent.class, (task) -> configureCopyContent(task, path, configuration, outputDirectory));
		configureAntora(addInputFrom(copyAntoraContent, configuration.getName()));
		configurePlaybookGeneration(this::addToZipContentsCollectorDependencies);
		publish(copyAntoraContent);
	}

	void publish(TaskProvider<? extends Task> producer) {
		getProject().getExtensions()
			.getByType(PublishingExtension.class)
			.getPublications()
			.withType(MavenPublication.class)
			.configureEach((mavenPublication) -> addPublishedMavenArtifact(mavenPublication, producer));
	}

	private void configureCopyContent(CopyAntoraContent task, String path, Configuration configuration,
			Provider<Directory> outputDirectory) {
		task.setDescription(
				"Syncs the %s Antora %s content from %s.".formatted(getName(), toDescription(getType()), path));
		task.setSource(configuration);
		task.getOutputFile().set(outputDirectory.map(this::getContentZipFile));
	}

	private void addToZipContentsCollectorDependencies(GenerateAntoraPlaybook task) {
		task.getAntoraExtensions().getZipContentsCollector().getDependencies().add(getName());
	}

	private void addPublishedMavenArtifact(MavenPublication mavenPublication, TaskProvider<?> producer) {
		if ("maven".equals(mavenPublication.getName())) {
			String classifier = "%s-%s-content".formatted(getName(), getType());
			mavenPublication.artifact(producer, (mavenArtifact) -> mavenArtifact.setClassifier(classifier));
		}
	}

	private RegularFile getContentZipFile(Directory dir) {
		Object version = getProject().getVersion();
		return dir.file("spring-boot-docs-%s-%s-%s-content.zip".formatted(version, getName(), getType()));
	}

	private static String toDescription(String input) {
		return input.replace("-", " ");
	}

	private Configuration createConfiguration(String name, String description) {
		return getProject().getConfigurations()
			.create(configurationName(name, "Antora%sContent", getType()),
					(configuration) -> configuration.setDescription(description.formatted(getName(), getType())));
	}

}
