/*
 * Copyright 2012-present the original author or authors.
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
