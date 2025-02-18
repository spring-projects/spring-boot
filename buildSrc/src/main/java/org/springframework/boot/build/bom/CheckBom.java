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

package org.springframework.boot.build.bom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.ManagedDependencies.Difference;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Checks the validity of a bom.
 *
 * @author Andy Wilkinson
 * @author Wick Dynex
 */
public abstract class CheckBom extends DefaultTask {

	private final ConfigurationContainer configurations;

	private final DependencyHandler dependencies;

	private final BomExtension bom;

	@Inject
	public CheckBom(BomExtension bom) {
		this.bom = bom;
		this.configurations = getProject().getConfigurations();
		this.dependencies = getProject().getDependencies();
	}

	@TaskAction
	void checkBom() {
		List<String> errors = new ArrayList<>();
		for (Library library : this.bom.getLibraries()) {
			checkLibrary(library, errors);
		}
		if (!errors.isEmpty()) {
			System.out.println();
			errors.forEach(System.out::println);
			System.out.println();
			throw new GradleException("Bom check failed. See previous output for details.");
		}
	}

	private void checkLibrary(Library library, List<String> errors) {
		List<String> libraryErrors = new ArrayList<>();
		checkExclusions(library, libraryErrors);
		checkProhibitedVersions(library, libraryErrors);
		checkVersionAlignment(library, libraryErrors);
		checkDependencyManagementAlignment(library, libraryErrors);
		if (!libraryErrors.isEmpty()) {
			errors.add(library.getName());
			for (String libraryError : libraryErrors) {
				errors.add("    - " + libraryError);
			}
		}
	}

	private void checkExclusions(Library library, List<String> errors) {
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				if (!module.getExclusions().isEmpty()) {
					checkExclusions(group.getId(), module, library.getVersion().getVersion(), errors);
				}
			}
		}
	}

	private void checkExclusions(String groupId, Module module, DependencyVersion version, List<String> errors) {
		Set<String> resolved = this.configurations
			.detachedConfiguration(this.dependencies.create(groupId + ":" + module.getName() + ":" + version))
			.getResolvedConfiguration()
			.getResolvedArtifacts()
			.stream()
			.map((artifact) -> artifact.getModuleVersion().getId())
			.map((id) -> id.getGroup() + ":" + id.getModule().getName())
			.collect(Collectors.toSet());
		Set<String> exclusions = module.getExclusions()
			.stream()
			.map((exclusion) -> exclusion.getGroupId() + ":" + exclusion.getArtifactId())
			.collect(Collectors.toSet());
		Set<String> unused = new TreeSet<>();
		for (String exclusion : exclusions) {
			if (!resolved.contains(exclusion)) {
				if (exclusion.endsWith(":*")) {
					String group = exclusion.substring(0, exclusion.indexOf(':') + 1);
					if (resolved.stream().noneMatch((candidate) -> candidate.startsWith(group))) {
						unused.add(exclusion);
					}
				}
				else {
					unused.add(exclusion);
				}
			}
		}
		exclusions.removeAll(resolved);
		if (!unused.isEmpty()) {
			errors.add("Unnecessary exclusions on " + groupId + ":" + module.getName() + ": " + exclusions);
		}
	}

	private void checkProhibitedVersions(Library library, List<String> errors) {
		ArtifactVersion currentVersion = new DefaultArtifactVersion(library.getVersion().getVersion().toString());
		for (ProhibitedVersion prohibited : library.getProhibitedVersions()) {
			if (prohibited.isProhibited(library.getVersion().getVersion().toString())) {
				errors.add("Current version " + currentVersion + " is prohibited");
			}
			else {
				VersionRange versionRange = prohibited.getRange();
				if (versionRange != null) {
					for (Restriction restriction : versionRange.getRestrictions()) {
						ArtifactVersion upperBound = restriction.getUpperBound();
						if (upperBound == null) {
							return;
						}
						int comparison = currentVersion.compareTo(upperBound);
						if ((restriction.isUpperBoundInclusive() && comparison <= 0)
								|| ((!restriction.isUpperBoundInclusive()) && comparison < 0)) {
							return;
						}
					}
					errors.add("Version range " + versionRange + " is ineffective as the current version, "
							+ currentVersion + ", is greater than its upper bound");
				}
			}
		}
	}

	private void checkVersionAlignment(Library library, List<String> errors) {
		VersionAlignment versionAlignment = library.getVersionAlignment();
		if (versionAlignment == null) {
			return;
		}
		Set<String> alignedVersions = versionAlignment.resolve();
		if (alignedVersions.size() == 1) {
			String alignedVersion = alignedVersions.iterator().next();
			if (!alignedVersion.equals(library.getVersion().getVersion().toString())) {
				errors.add("Version " + library.getVersion().getVersion() + " is misaligned. It should be "
						+ alignedVersion + ".");
			}
		}
		else {
			if (alignedVersions.isEmpty()) {
				errors.add("Version alignment requires a single version but none were found.");
			}
			else {
				errors.add("Version alignment requires a single version but " + alignedVersions.size() + " were found: "
						+ alignedVersions + ".");
			}
		}
	}

	private void checkDependencyManagementAlignment(Library library, List<String> errors) {
		String alignsWithBom = library.getAlignsWithBom();
		if (alignsWithBom == null) {
			return;
		}
		File bom = resolveBom(library, alignsWithBom);
		ManagedDependencies managedByBom = ManagedDependencies.ofBom(bom);
		ManagedDependencies managedByLibrary = ManagedDependencies.ofLibrary(library);
		Difference diff = managedByBom.diff(managedByLibrary);
		if (!diff.isEmpty()) {
			String error = "Dependency management does not align with " + library.getAlignsWithBom() + ":";
			if (!diff.missing().isEmpty()) {
				error = error + "%n        - Missing:%n            %s"
					.formatted(String.join("\n            ", diff.missing()));
			}
			if (!diff.unexpected().isEmpty()) {
				error = error + "%n        - Unexpected:%n            %s"
					.formatted(String.join("\n            ", diff.unexpected()));
			}
			errors.add(error);
		}
	}

	private File resolveBom(Library library, String alignsWithBom) {
		String coordinates = alignsWithBom + ":" + library.getVersion().getVersion() + "@pom";
		Set<ResolvedArtifact> artifacts = this.configurations
			.detachedConfiguration(this.dependencies.create(coordinates))
			.getResolvedConfiguration()
			.getResolvedArtifacts();
		if (artifacts.size() != 1) {
			throw new IllegalStateException("Expected a single artifact but '%s' resolved to %d artifacts"
				.formatted(coordinates, artifacts.size()));
		}
		return artifacts.iterator().next().getFile();
	}

}
