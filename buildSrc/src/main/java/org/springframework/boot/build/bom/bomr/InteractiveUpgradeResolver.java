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

package org.springframework.boot.build.bom.bomr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.internal.tasks.userinput.UserInputHandler;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.VersionAlignment;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Interactive {@link UpgradeResolver} that uses command line input to choose the upgrades
 * to apply.
 *
 * @author Andy Wilkinson
 */
public final class InteractiveUpgradeResolver implements UpgradeResolver {

	private final UserInputHandler userInputHandler;

	private final LibraryUpdateResolver libraryUpdateResolver;

	InteractiveUpgradeResolver(UserInputHandler userInputHandler, LibraryUpdateResolver libraryUpdateResolver) {
		this.userInputHandler = userInputHandler;
		this.libraryUpdateResolver = libraryUpdateResolver;
	}

	@Override
	public List<Upgrade> resolveUpgrades(Collection<Library> librariesToUpgrade, Collection<Library> libraries) {
		Map<String, Library> librariesByName = new HashMap<>();
		for (Library library : libraries) {
			librariesByName.put(library.getName(), library);
		}
		try {
			return this.libraryUpdateResolver.findLibraryUpdates(librariesToUpgrade, librariesByName)
				.stream()
				.map(this::resolveUpgrade)
				.filter(Objects::nonNull)
				.toList();
		}
		catch (UpgradesInterruptedException ex) {
			return Collections.emptyList();
		}
	}

	private Upgrade resolveUpgrade(LibraryWithVersionOptions libraryWithVersionOptions) {
		Library library = libraryWithVersionOptions.getLibrary();
		List<VersionOption> versionOptions = libraryWithVersionOptions.getVersionOptions();
		if (versionOptions.isEmpty()) {
			return null;
		}
		VersionOption defaultOption = defaultOption(library);
		VersionOption selected = selectOption(defaultOption, library, versionOptions);
		return (selected.equals(defaultOption)) ? null : selected.upgrade(library);
	}

	private VersionOption defaultOption(Library library) {
		VersionAlignment alignment = library.getVersionAlignment();
		Set<String> alignedVersions = (alignment != null) ? alignment.resolve() : null;
		if (alignedVersions != null && alignedVersions.size() == 1) {
			DependencyVersion alignedVersion = DependencyVersion.parse(alignedVersions.iterator().next());
			if (alignedVersion.equals(library.getVersion().getVersion())) {
				return new VersionOption.AlignedVersionOption(alignedVersion, alignment);
			}
		}
		return new VersionOption(library.getVersion().getVersion());
	}

	private VersionOption selectOption(VersionOption defaultOption, Library library,
			List<VersionOption> versionOptions) {
		VersionOption selected = this.userInputHandler.askUser((questions) -> {
			String question = library.getNameAndVersion();
			List<VersionOption> options = new ArrayList<>();
			options.add(defaultOption);
			options.addAll(versionOptions);
			return questions.selectOption(question, options, defaultOption);
		}).get();
		if (this.userInputHandler.interrupted()) {
			throw new UpgradesInterruptedException();
		}
		return selected;
	}

	static class UpgradesInterruptedException extends RuntimeException {

	}

}
