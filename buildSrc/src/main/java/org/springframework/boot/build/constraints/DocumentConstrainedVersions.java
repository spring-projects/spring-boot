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

package org.springframework.boot.build.constraints;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.constraints.ExtractVersionConstraints.ConstrainedVersion;

/**
 * Task for documenting a platform's constrained versions.
 *
 * @author Andy Wilkinson
 */
public abstract class DocumentConstrainedVersions extends DefaultTask {

	@Input
	public abstract SetProperty<ConstrainedVersion> getConstrainedVersions();

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	public void documentConstrainedVersions() throws IOException {
		File outputFile = getOutputFile().get().getAsFile();
		outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
			writer.println("|===");
			writer.println("| Group ID | Artifact ID | Version");
			for (ConstrainedVersion constrainedVersion : getConstrainedVersions().get()) {
				writer.println();
				writer.printf("| `%s`%n", constrainedVersion.getGroup());
				writer.printf("| `%s`%n", constrainedVersion.getArtifact());
				writer.printf("| `%s`%n", constrainedVersion.getVersion());
			}
			writer.println("|===");
		}
	}

}
