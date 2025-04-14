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

package org.springframework.boot.build.bom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.ImportedBom;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.PermittedDependency;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.ResolvedBom.Bom;
import org.springframework.boot.build.bom.ResolvedBom.Id;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Checks the validity of a bom.
 *
 * @author Andy Wilkinson
 * @author Wick Dynex
 */
public abstract class CheckBom extends DefaultTask {

	private final BomExtension bom;

	private final List<LibraryCheck> checks;

	@Inject
	public CheckBom(BomExtension bom) {
		ConfigurationContainer configurations = getProject().getConfigurations();
		DependencyHandler dependencies = getProject().getDependencies();
		Provider<ResolvedBom> resolvedBom = getResolvedBomFile().map(RegularFile::getAsFile).map(ResolvedBom::readFrom);
		this.checks = List.of(new CheckExclusions(configurations, dependencies), new CheckProhibitedVersions(),
				new CheckVersionAlignment(),
				new CheckDependencyManagementAlignment(resolvedBom, configurations, dependencies),
				new CheckForUnwantedDependencyManagement(resolvedBom));
		this.bom = bom;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getResolvedBomFile();

	@TaskAction
	void checkBom() {
		List<String> errors = new ArrayList<>();
		for (Library library : this.bom.getLibraries()) {
			errors.addAll(checkLibrary(library));
		}
		if (!errors.isEmpty()) {
			System.out.println();
			errors.forEach(System.out::println);
			System.out.println();
			throw new VerificationException("Bom check failed. See previous output for details.");
		}
	}

	private List<String> checkLibrary(Library library) {
		List<String> libraryErrors = new ArrayList<>();
		this.checks.stream().flatMap((check) -> check.check(library).stream()).forEach(libraryErrors::add);
		List<String> errors = new ArrayList<>();
		if (!libraryErrors.isEmpty()) {
			errors.add(library.getName());
			for (String libraryError : libraryErrors) {
				errors.add("    - " + libraryError);
			}
		}
		return errors;
	}

	private interface LibraryCheck {

		List<String> check(Library library);

	}

	private static final class CheckExclusions implements LibraryCheck {

		private final ConfigurationContainer configurations;

		private final DependencyHandler dependencies;

		private CheckExclusions(ConfigurationContainer configurations, DependencyHandler dependencies) {
			this.configurations = configurations;
			this.dependencies = dependencies;
		}

		@Override
		public List<String> check(Library library) {
			List<String> errors = new ArrayList<>();
			for (Group group : library.getGroups()) {
				for (Module module : group.getModules()) {
					if (!module.getExclusions().isEmpty()) {
						checkExclusions(group.getId(), module, library.getVersion().getVersion(), errors);
					}
				}
			}
			return errors;
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

	}

	private static final class CheckProhibitedVersions implements LibraryCheck {

		@Override
		public List<String> check(Library library) {
			List<String> errors = new ArrayList<>();
			ArtifactVersion currentVersion = new DefaultArtifactVersion(library.getVersion().getVersion().toString());
			for (ProhibitedVersion prohibited : library.getProhibitedVersions()) {
				if (prohibited.isProhibited(library.getVersion().getVersion().toString())) {
					errors.add("Current version " + currentVersion + " is prohibited");
				}
				else {
					VersionRange versionRange = prohibited.getRange();
					if (versionRange != null) {
						check(currentVersion, versionRange, errors);
					}
				}
			}
			return errors;
		}

		private void check(ArtifactVersion currentVersion, VersionRange versionRange, List<String> errors) {
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
			errors.add("Version range " + versionRange + " is ineffective as the current version, " + currentVersion
					+ ", is greater than its upper bound");
		}

	}

	private static final class CheckVersionAlignment implements LibraryCheck {

		@Override
		public List<String> check(Library library) {
			List<String> errors = new ArrayList<>();
			VersionAlignment versionAlignment = library.getVersionAlignment();
			if (versionAlignment != null) {
				check(versionAlignment, library, errors);
			}
			return errors;
		}

		private void check(VersionAlignment versionAlignment, Library library, List<String> errors) {
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
					errors.add("Version alignment requires a single version but " + alignedVersions.size()
							+ " were found: " + alignedVersions + ".");
				}
			}
		}

	}

	private abstract static class ResolvedLibraryCheck implements LibraryCheck {

		private final Provider<ResolvedBom> resolvedBom;

		private ResolvedLibraryCheck(Provider<ResolvedBom> resolvedBom) {
			this.resolvedBom = resolvedBom;
		}

		@Override
		public List<String> check(Library library) {
			ResolvedLibrary resolvedLibrary = getResolvedLibrary(library);
			return check(library, resolvedLibrary);
		}

		protected abstract List<String> check(Library library, ResolvedLibrary resolvedLibrary);

		private ResolvedLibrary getResolvedLibrary(Library library) {
			ResolvedBom resolvedBom = this.resolvedBom.get();
			Optional<ResolvedLibrary> resolvedLibrary = resolvedBom.libraries()
				.stream()
				.filter((candidate) -> candidate.name().equals(library.getName()))
				.findFirst();
			if (!resolvedLibrary.isPresent()) {
				throw new RuntimeException("Library '%s' not found in resolved bom".formatted(library.getName()));
			}
			return resolvedLibrary.get();
		}

	}

	private static final class CheckDependencyManagementAlignment extends ResolvedLibraryCheck {

		private final BomResolver bomResolver;

		private CheckDependencyManagementAlignment(Provider<ResolvedBom> resolvedBom,
				ConfigurationContainer configurations, DependencyHandler dependencies) {
			super(resolvedBom);
			this.bomResolver = new BomResolver(configurations, dependencies);
		}

		@Override
		public List<String> check(Library library, ResolvedLibrary resolvedLibrary) {
			List<String> errors = new ArrayList<>();
			String alignsWithBom = library.getAlignsWithBom();
			if (alignsWithBom != null) {
				Bom mavenBom = this.bomResolver
					.resolveMavenBom(alignsWithBom + ":" + library.getVersion().getVersion());
				checkDependencyManagementAlignment(resolvedLibrary, mavenBom, errors);
			}
			return errors;
		}

		private void checkDependencyManagementAlignment(ResolvedLibrary library, Bom mavenBom, List<String> errors) {
			List<Id> managedByLibrary = library.managedDependencies();
			List<Id> managedByBom = managedDependenciesOf(mavenBom);

			List<Id> missing = new ArrayList<>(managedByBom);
			missing.removeAll(managedByLibrary);

			List<Id> unexpected = new ArrayList<>(managedByLibrary);
			unexpected.removeAll(managedByBom);
			if (missing.isEmpty() && unexpected.isEmpty()) {
				return;
			}
			String error = "Dependency management does not align with " + mavenBom.id() + ":";
			if (!missing.isEmpty()) {
				error = error + "%n        - Missing:%n            %s".formatted(String.join("\n            ",
						missing.stream().map((dependency) -> dependency.toString()).toList()));
			}
			if (!unexpected.isEmpty()) {
				error = error + "%n        - Unexpected:%n            %s".formatted(String.join("\n            ",
						unexpected.stream().map((dependency) -> dependency.toString()).toList()));
			}
			errors.add(error);
		}

		private List<Id> managedDependenciesOf(Bom mavenBom) {
			List<Id> managedDependencies = new ArrayList<>();
			managedDependencies.addAll(mavenBom.managedDependencies());
			if (mavenBom.parent() != null) {
				managedDependencies.addAll(managedDependenciesOf(mavenBom.parent()));
			}
			for (Bom importedBom : mavenBom.importedBoms()) {
				managedDependencies.addAll(managedDependenciesOf(importedBom));
			}
			return managedDependencies;
		}

	}

	private static final class CheckForUnwantedDependencyManagement extends ResolvedLibraryCheck {

		private CheckForUnwantedDependencyManagement(Provider<ResolvedBom> resolvedBom) {
			super(resolvedBom);
		}

		@Override
		public List<String> check(Library library, ResolvedLibrary resolvedLibrary) {
			Map<String, Set<String>> unwanted = findUnwantedDependencyManagement(library, resolvedLibrary);
			List<String> errors = new ArrayList<>();
			if (!unwanted.isEmpty()) {
				StringBuilder error = new StringBuilder("Unwanted dependency management:");
				unwanted.forEach((bom, dependencies) -> {
					error.append("%n        - %s:".formatted(bom));
					error.append("%n            - %s".formatted(String.join("\n            - ", dependencies)));
				});
				errors.add(error.toString());
			}
			Map<String, Set<String>> unnecessary = findUnnecessaryPermittedDependencies(library, resolvedLibrary);
			if (!unnecessary.isEmpty()) {
				StringBuilder error = new StringBuilder("Dependencies permitted unnecessarily:");
				unnecessary.forEach((bom, dependencies) -> {
					error.append("%n        - %s:".formatted(bom));
					error.append("%n            - %s".formatted(String.join("\n            - ", dependencies)));
				});
				errors.add(error.toString());
			}
			return errors;
		}

		private Map<String, Set<String>> findUnwantedDependencyManagement(Library library,
				ResolvedLibrary resolvedLibrary) {
			Map<String, Set<String>> unwanted = new LinkedHashMap<>();
			for (Bom bom : resolvedLibrary.importedBoms()) {
				Set<String> notPermitted = new TreeSet<>();
				Set<Id> managedDependencies = managedDependenciesOf(bom);
				managedDependencies.stream()
					.filter((dependency) -> unwanted(bom, dependency, findPermittedDependencies(library, bom)))
					.map(Id::toString)
					.forEach(notPermitted::add);
				if (!notPermitted.isEmpty()) {
					unwanted.put(bom.id().artifactId(), notPermitted);
				}
			}
			return unwanted;
		}

		private List<PermittedDependency> findPermittedDependencies(Library library, Bom bom) {
			for (Group group : library.getGroups()) {
				for (ImportedBom importedBom : group.getBoms()) {
					if (importedBom.name().equals(bom.id().artifactId()) && group.getId().equals(bom.id().groupId())) {
						return importedBom.permittedDependencies();
					}
				}
			}
			return Collections.emptyList();
		}

		private Set<Id> managedDependenciesOf(Bom bom) {
			Set<Id> managedDependencies = new TreeSet<>();
			if (bom != null) {
				managedDependencies.addAll(bom.managedDependencies());
				managedDependencies.addAll(managedDependenciesOf(bom.parent()));
				for (Bom importedBom : bom.importedBoms()) {
					managedDependencies.addAll(managedDependenciesOf(importedBom));
				}
			}
			return managedDependencies;
		}

		private boolean unwanted(Bom bom, Id managedDependency, List<PermittedDependency> permittedDependencies) {
			if (bom.id().groupId().equals(managedDependency.groupId())
					|| managedDependency.groupId().startsWith(bom.id().groupId() + ".")) {
				return false;
			}
			for (PermittedDependency permittedDependency : permittedDependencies) {
				if (permittedDependency.artifactId().equals(managedDependency.artifactId())
						&& permittedDependency.groupId().equals(managedDependency.groupId())) {
					return false;
				}
			}
			return true;
		}

		private Map<String, Set<String>> findUnnecessaryPermittedDependencies(Library library,
				ResolvedLibrary resolvedLibrary) {
			Map<String, Set<String>> unnecessary = new HashMap<>();
			for (Bom bom : resolvedLibrary.importedBoms()) {
				Set<String> permittedDependencies = findPermittedDependencies(library, bom).stream()
					.map((dependency) -> dependency.groupId() + ":" + dependency.artifactId())
					.collect(Collectors.toCollection(TreeSet::new));
				Set<String> dependencies = managedDependenciesOf(bom).stream()
					.map((dependency) -> dependency.groupId() + ":" + dependency.artifactId())
					.collect(Collectors.toCollection(TreeSet::new));
				permittedDependencies.removeAll(dependencies);
				if (!permittedDependencies.isEmpty()) {
					unnecessary.put(bom.id().artifactId(), permittedDependencies);
				}
			}
			return unnecessary;
		}

	}

}
