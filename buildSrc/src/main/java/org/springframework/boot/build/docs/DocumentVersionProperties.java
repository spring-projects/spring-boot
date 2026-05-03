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

package org.springframework.boot.build.docs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;

/**
 * Task for documenting {@link ResolvedBom boms'} version properties.
 *
 * @author Christoph Dreis
 * @author Andy Wilkinson
 */
public abstract class DocumentVersionProperties extends DefaultTask {

	private FileCollection resolvedBoms;

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileCollection getResolvedBoms() {
		return this.resolvedBoms;
	}

	public void setResolvedBoms(FileCollection resolvedBoms) {
		this.resolvedBoms = resolvedBoms;
	}

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	public void documentVersionProperties() throws IOException {
		List<ResolvedLibrary> libraries = this.resolvedBoms.getFiles()
			.stream()
			.map(ResolvedBom::readFrom)
			.flatMap((resolvedBom) -> resolvedBom.libraries().stream())
			.sorted((l1, l2) -> l1.name().compareToIgnoreCase(l2.name()))
			.toList();
		File outputFile = getOutputFile().getAsFile().get();
		outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
			writer.println("|===");
			writer.println("| Library | Version Property");
			for (ResolvedLibrary library : libraries) {
				writer.println();
				writer.printf("| `%s`%n", library.name());
				writer.printf("| `%s`%n", library.versionProperty());
			}
			writer.println("|===");
		}
	}

}
