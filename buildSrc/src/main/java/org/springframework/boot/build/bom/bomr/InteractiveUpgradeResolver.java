/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build.bom.bomr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;
import org.springframework.boot.build.bom.Library.ProhibitedVersion;
import org.springframework.boot.build.bom.UpgradePolicy;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.util.StringUtils;

/**
 * Interactive {@link UpgradeResolver} that uses command line input to choose the upgrades
 * to apply.
 *
 * @author Andy Wilkinson
 */
public final class InteractiveUpgradeResolver implements UpgradeResolver {

	private final VersionResolver versionResolver;

	private final UpgradePolicy upgradePolicy;

	private final UserInputHandler userInputHandler;

	InteractiveUpgradeResolver(VersionResolver versionResolver, UpgradePolicy upgradePolicy,
			UserInputHandler userInputHandler) {
		this.versionResolver = versionResolver;
		this.upgradePolicy = upgradePolicy;
		this.userInputHandler = userInputHandler;
	}

	@Override
	public List<Upgrade> resolveUpgrades(Collection<Library> libraries) {
		return libraries.stream().map(this::resolveUpgrade).filter((upgrade) -> upgrade != null)
				.collect(Collectors.toList());
	}

	private Upgrade resolveUpgrade(Library library) {
		Map<String, SortedSet<DependencyVersion>> moduleVersions = new LinkedHashMap<>();
		for (Group group : library.getGroups()) {
			for (Module module : group.getModules()) {
				moduleVersions.put(group.getId() + ":" + module.getName(),
						getLaterVersionsForModule(group.getId(), module.getName(), library.getVersion()));
			}
			for (String bom : group.getBoms()) {
				moduleVersions.put(group.getId() + ":" + bom,
						getLaterVersionsForModule(group.getId(), bom, library.getVersion()));
			}
			for (String plugin : group.getPlugins()) {
				moduleVersions.put(group.getId() + ":" + plugin,
						getLaterVersionsForModule(group.getId(), plugin, library.getVersion()));
			}
		}
		List<DependencyVersion> allVersions = moduleVersions.values().stream().flatMap(SortedSet::stream).distinct()
				.filter((dependencyVersion) -> isPermitted(dependencyVersion, library.getProhibitedVersions()))
				.collect(Collectors.toList());
		if (allVersions.isEmpty()) {
			return null;
		}
		List<VersionOption> versionOptions = allVersions.stream()
				.map((version) -> new VersionOption(version, getMissingModules(moduleVersions, version)))
				.collect(Collectors.toList());
		VersionOption current = new VersionOption(library.getVersion(), Collections.emptyList());
		VersionOption selected = this.userInputHandler.selectOption(library.getName() + " " + library.getVersion(),
				versionOptions, current);
		return (selected.equals(current)) ? null : new Upgrade(library, selected.version);
	}

	private boolean isPermitted(DependencyVersion dependencyVersion, List<ProhibitedVersion> prohibitedVersions) {
		if (prohibitedVersions.isEmpty()) {
			return true;
		}
		for (ProhibitedVersion prohibitedVersion : prohibitedVersions) {
			if (prohibitedVersion.getRange()
					.containsVersion(new DefaultArtifactVersion(dependencyVersion.toString()))) {
				return false;
			}
		}
		return true;
	}

	private List<String> getMissingModules(Map<String, SortedSet<DependencyVersion>> moduleVersions,
			DependencyVersion version) {
		List<String> missingModules = new ArrayList<>();
		moduleVersions.forEach((name, versions) -> {
			if (!versions.contains(version)) {
				missingModules.add(name);
			}
		});
		return missingModules;
	}

	private SortedSet<DependencyVersion> getLaterVersionsForModule(String groupId, String artifactId,
			DependencyVersion currentVersion) {
		SortedSet<DependencyVersion> versions = this.versionResolver.resolveVersions(groupId, artifactId);
		versions.removeIf((candidate) -> !this.upgradePolicy.test(candidate, currentVersion));
		return versions;
	}

	private static final class VersionOption {

		private final DependencyVersion version;

		private final List<String> missingModules;

		private VersionOption(DependencyVersion version, List<String> missingModules) {
			this.version = version;
			this.missingModules = missingModules;
		}

		@Override
		public String toString() {
			if (this.missingModules.isEmpty()) {
				return this.version.toString();
			}
			return this.version + " (some modules are missing: "
					+ StringUtils.collectionToDelimitedString(this.missingModules, ", ") + ")";
		}

	}

}
