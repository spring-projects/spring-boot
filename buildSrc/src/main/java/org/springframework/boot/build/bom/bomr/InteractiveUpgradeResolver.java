/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.internal.tasks.userinput.UserInputHandler;

import org.springframework.boot.build.bom.Library;

/**
 * Interactive {@link UpgradeResolver} that uses command line input to choose the upgrades
 * to apply.
 *
 * @author Andy Wilkinson
 */
public final class InteractiveUpgradeResolver implements UpgradeResolver {

	private final UserInputHandler userInputHandler;

	private final LibraryUpdateResolver libraryUpdateResolver;

	/**
     * Constructs a new InteractiveUpgradeResolver with the specified UserInputHandler and LibraryUpdateResolver.
     * 
     * @param userInputHandler the UserInputHandler to handle user input
     * @param libraryUpdateResolver the LibraryUpdateResolver to resolve library updates
     */
    InteractiveUpgradeResolver(UserInputHandler userInputHandler, LibraryUpdateResolver libraryUpdateResolver) {
		this.userInputHandler = userInputHandler;
		this.libraryUpdateResolver = libraryUpdateResolver;
	}

	/**
     * Resolves the upgrades for a collection of libraries to be upgraded, based on the available libraries.
     * 
     * @param librariesToUpgrade the collection of libraries to be upgraded
     * @param libraries the collection of available libraries
     * @return a list of Upgrade objects representing the resolved upgrades
     */
    @Override
	public List<Upgrade> resolveUpgrades(Collection<Library> librariesToUpgrade, Collection<Library> libraries) {
		Map<String, Library> librariesByName = new HashMap<>();
		for (Library library : libraries) {
			librariesByName.put(library.getName(), library);
		}
		List<LibraryWithVersionOptions> libraryUpdates = this.libraryUpdateResolver
			.findLibraryUpdates(librariesToUpgrade, librariesByName);
		return libraryUpdates.stream().map(this::resolveUpgrade).filter(Objects::nonNull).toList();
	}

	/**
     * Resolves the upgrade for a library with version options.
     * 
     * @param libraryWithVersionOptions the library with version options
     * @return the upgrade object if a new version is selected, null otherwise
     */
    private Upgrade resolveUpgrade(LibraryWithVersionOptions libraryWithVersionOptions) {
		if (libraryWithVersionOptions.getVersionOptions().isEmpty()) {
			return null;
		}
		VersionOption current = new VersionOption(libraryWithVersionOptions.getLibrary().getVersion().getVersion());
		VersionOption selected = this.userInputHandler.selectOption(
				libraryWithVersionOptions.getLibrary().getName() + " "
						+ libraryWithVersionOptions.getLibrary().getVersion().getVersion(),
				libraryWithVersionOptions.getVersionOptions(), current);
		return (selected.equals(current)) ? null
				: new Upgrade(libraryWithVersionOptions.getLibrary(), selected.getVersion());
	}

}
