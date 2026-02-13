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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
			writer.println("| Group ID | Artifact ID | Version | Version Property");
			Map<Id, Set<String>> managedCoordinates = new TreeMap<>((id1, id2) -> {
				int groupComparison = id1.groupId().compareTo(id2.groupId());
				if (groupComparison != 0) {
					return groupComparison;
				}
				int artifactComparison = id1.artifactId().compareTo(id2.artifactId());
				if (artifactComparison != 0) {
					return artifactComparison;
				}
				return id1.version().compareTo(id2.version());
			});
			for (File file : getResolvedBoms().getFiles()) {
				process(ResolvedBom.readFrom(file), managedCoordinates);
			}
			for (Map.Entry<Id, Set<String>> entry : managedCoordinates.entrySet()) {
				Id id = entry.getKey();
				writer.println();
				writer.printf("| `%s`%n", id.groupId());
				writer.printf("| `%s`%n", id.artifactId());
				writer.printf("| `%s`%n", id.version());
				writer.println(formatVersionProperties(entry.getValue()));
			}
			writer.println("|===");
		}
	}

	private void process(ResolvedBom resolvedBom, Map<Id, Set<String>> managedCoordinates) {
		for (ResolvedLibrary library : resolvedBom.libraries()) {
			String versionProperty = library.versionProperty();
			addManagedDependencies(managedCoordinates, versionProperty, library.managedDependencies());
			for (Bom importedBom : library.importedBoms()) {
				process(importedBom, managedCoordinates, versionProperty);
			}
		}
	}

	private void process(Bom bom, Map<Id, Set<String>> managedCoordinates, String versionProperty) {
		addManagedDependencies(managedCoordinates, versionProperty, bom.managedDependencies());
		for (Bom importedBom : bom.importedBoms()) {
			process(importedBom, managedCoordinates, versionProperty);
		}
		Bom parent = bom.parent();
		if (parent != null) {
			process(parent, managedCoordinates, versionProperty);
		}
	}

	private void addManagedDependencies(Map<Id, Set<String>> managedCoordinates, String versionProperty,
			Collection<Id> managedDependencies) {
		for (Id managedDependency : managedDependencies) {
			Set<String> properties = managedCoordinates.computeIfAbsent(managedDependency, (id) -> new TreeSet<>());
			if (versionProperty != null && !versionProperty.isBlank()) {
				properties.add(versionProperty);
			}
		}
	}

	private String formatVersionProperties(Set<String> versionProperties) {
		if (versionProperties.isEmpty()) {
			return "|";
		}
		String formattedProperties = versionProperties.stream()
			.map((property) -> "`%s`".formatted(property))
			.collect(Collectors.joining(", "));
		return "| " + formattedProperties;
	}

}
