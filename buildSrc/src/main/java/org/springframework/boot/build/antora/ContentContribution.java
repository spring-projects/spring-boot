/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

/**
 * A contribution of content to Antora.
 *
 * @author Andy Wilkinson
 */
abstract class ContentContribution extends Contribution {

	private final String type;

	protected ContentContribution(Project project, String name, String type) {
		super(project, name);
		this.type = type;
	}

	protected String getType() {
		return this.type;
	}

	abstract void produceFrom(CopySpec copySpec);

	protected TaskProvider<? extends Task> configureProduction(CopySpec copySpec) {
		TaskContainer tasks = getProject().getTasks();
		TaskProvider<Zip> zipContent = tasks.register(taskName("zip", "%sAntora%sContent", getName(), this.type),
				Zip.class, (zip) -> {
					zip.getDestinationDirectory()
						.set(getProject().getLayout().getBuildDirectory().dir("generated/docs/antora-content"));
					zip.getArchiveClassifier().set("%s-%s-content".formatted(getName(), this.type));
					zip.with(copySpec);
					zip.setDescription("Creates a zip archive of the %s Antora %s content.".formatted(getName(),
							toDescription(this.type)));
				});
		configureAntora(addInputFrom(zipContent, zipContent.getName()));
		return zipContent;
	}

	private static String toDescription(String input) {
		return input.replace("-", " ");
	}

}
