/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.Set;
import java.util.TreeSet;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.boot.build.bom.ResolvedBom.Bom;
import org.springframework.boot.build.bom.ResolvedBom.Id;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;

/**
 * Task for documenting {@link ResolvedBom boms'} managed dependencies.
 *
 * @author Andy Wilkinson
 */
public abstract class DocumentManagedDependencies extends DefaultTask {

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
	public void documentConstrainedVersions() throws IOException {
		File outputFile = getOutputFile().get().getAsFile();
		outputFile.getParentFile().mkdirs();
		try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
			writer.println("|===");
			writer.println("| Group ID | Artifact ID | Version");
			Set<Id> managedCoordinates = new TreeSet<>((id1, id2) -> {
				int result = id1.groupId().compareTo(id2.groupId());
				if (result != 0) {
					return result;
				}
				return id1.artifactId().compareTo(id2.artifactId());
			});
			for (File file : getResolvedBoms().getFiles()) {
				managedCoordinates.addAll(process(ResolvedBom.readFrom(file)));
			}
			for (Id id : managedCoordinates) {
				writer.println();
				writer.printf("| `%s`%n", id.groupId());
				writer.printf("| `%s`%n", id.artifactId());
				writer.printf("| `%s`%n", id.version());
			}
			writer.println("|===");
		}
	}

	private Set<Id> process(ResolvedBom resolvedBom) {
		TreeSet<Id> managedCoordinates = new TreeSet<>();
		for (ResolvedLibrary library : resolvedBom.libraries()) {
			for (Id managedDependency : library.managedDependencies()) {
				managedCoordinates.add(managedDependency);
			}
			for (Bom importedBom : library.importedBoms()) {
				managedCoordinates.addAll(process(importedBom));
			}
		}
		return managedCoordinates;
	}

	private Set<Id> process(Bom bom) {
		TreeSet<Id> managedCoordinates = new TreeSet<>();
		bom.managedDependencies().stream().forEach(managedCoordinates::add);
		Bom parent = bom.parent();
		if (parent != null) {
			managedCoordinates.addAll(process(parent));
		}
		return managedCoordinates;
	}

}
