/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;

import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.ZipContentsCollector.AlwaysInclude;

/**
 * A contribution of aggregate content that cannot be consumed by other projects.
 *
 * @author Andy Wilkinson
 */
class LocalAggregateContentContribution extends ContentContribution {

	protected LocalAggregateContentContribution(Project project, String name) {
		super(project, name, "local-aggregate");
	}

	@Override
	void produceFrom(CopySpec copySpec) {
		super.configureProduction(copySpec);
		configurePlaybookGeneration(this::addToAlwaysInclude);
	}

	private void addToAlwaysInclude(GenerateAntoraPlaybook task) {
		task.getAntoraExtensions()
			.getZipContentsCollector()
			.getAlwaysInclude()
			.add(new AlwaysInclude(getName(), "local-aggregate-content"));
	}

}
