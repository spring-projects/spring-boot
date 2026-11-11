/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom;

import java.io.FileWriter;
import java.io.IOException;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} to create a {@link ResolvedBom resolved bom}.
 *
 * @author Andy Wilkinson
 */
public abstract class CreateResolvedBom extends DefaultTask {

	private final BomExtension bomExtension;

	private final BomResolver bomResolver;

	@Inject
	public CreateResolvedBom(BomExtension bomExtension) {
		getOutputs().upToDateWhen((spec) -> false);
		this.bomExtension = bomExtension;
		this.bomResolver = new BomResolver(getProject().getConfigurations(), getProject().getDependencies());
		getOutputFile().convention(getProject().getLayout().getBuildDirectory().file(getName() + "/resolved-bom.json"));
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	void createResolvedBom() throws IOException {
		ResolvedBom resolvedBom = this.bomResolver.resolve(this.bomExtension);
		try (FileWriter writer = new FileWriter(getOutputFile().get().getAsFile())) {
			resolvedBom.writeTo(writer);
		}
	}

}
