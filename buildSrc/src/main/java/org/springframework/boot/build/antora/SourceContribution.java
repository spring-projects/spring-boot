/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import org.springframework.boot.build.AntoraConventions;

/**
 * A contribution of source to Antora.
 *
 * @author Andy Wilkinson
 */
class SourceContribution extends Contribution {

	private static final String CONFIGURATION_NAME = "antoraSource";

	SourceContribution(Project project, String name) {
		super(project, name);
	}

	void produce() {
		Configuration antoraSource = getProject().getConfigurations().create(CONFIGURATION_NAME);
		TaskProvider<Zip> antoraSourceZip = getProject().getTasks().register("antoraSourceZip", Zip.class, (zip) -> {
			zip.getDestinationDirectory().set(getProject().getLayout().getBuildDirectory().dir("antora-source"));
			zip.from(AntoraConventions.ANTORA_SOURCE_DIR);
			zip.setDescription(
					"Creates a zip archive of the Antora source in %s.".formatted(AntoraConventions.ANTORA_SOURCE_DIR));
		});
		getProject().getArtifacts().add(antoraSource.getName(), antoraSourceZip);
	}

	void consumeFrom(String path) {
		Configuration configuration = createConfiguration(getName());
		DependencyHandler dependencies = getProject().getDependencies();
		dependencies.add(configuration.getName(),
				getProject().provider(() -> projectDependency(path, CONFIGURATION_NAME)));
		Provider<Directory> outputDirectory = outputDirectory("source", getName());
		TaskContainer tasks = getProject().getTasks();
		TaskProvider<SyncAntoraSource> syncSource = tasks.register(taskName("sync", "%s", configuration.getName()),
				SyncAntoraSource.class, (task) -> configureSyncSource(task, path, configuration, outputDirectory));
		configureAntora(addInputFrom(syncSource, configuration.getName()));
		configurePlaybookGeneration(
				(generatePlaybook) -> generatePlaybook.getContentSource().addStartPath(outputDirectory));
	}

	private void configureSyncSource(SyncAntoraSource task, String path, Configuration configuration,
			Provider<Directory> outputDirectory) {
		task.setDescription("Syncs the %s Antora source from %s.".formatted(getName(), path));
		task.setSource(configuration);
		task.getOutputDirectory().set(outputDirectory);
	}

	private Configuration createConfiguration(String name) {
		return getProject().getConfigurations().create(configurationName(name, "AntoraSource"));
	}

}
