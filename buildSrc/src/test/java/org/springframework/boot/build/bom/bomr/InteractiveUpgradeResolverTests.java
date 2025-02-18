/*
 * Copyright 2024-2024 the original author or authors.
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
import java.util.List;

import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.provider.Provider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.LibraryVersion;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InteractiveUpgradeResolver}.
 *
 * @author Phillip Webb
 */
class InteractiveUpgradeResolverTests {

	@Test
	void resolveUpgradeUpdateVersionNumberInLibrary() {
		UserInputHandler userInputHandler = mock(UserInputHandler.class);
		LibraryUpdateResolver libaryUpdateResolver = mock(LibraryUpdateResolver.class);
		InteractiveUpgradeResolver upgradeResolver = new InteractiveUpgradeResolver(userInputHandler,
				libaryUpdateResolver);
		List<Library> libraries = new ArrayList<>();
		DependencyVersion version = DependencyVersion.parse("1.0.0");
		LibraryVersion libraryVersion = new LibraryVersion(version);
		Library library = new Library("test", null, libraryVersion, null, null, false, null, null, null, null);
		libraries.add(library);
		List<Library> librariesToUpgrade = new ArrayList<>();
		librariesToUpgrade.add(library);
		List<LibraryWithVersionOptions> updates = new ArrayList<>();
		DependencyVersion updateVersion = DependencyVersion.parse("1.0.1");
		VersionOption versionOption = new VersionOption(updateVersion);
		updates.add(new LibraryWithVersionOptions(library, List.of(versionOption)));
		given(libaryUpdateResolver.findLibraryUpdates(any(), any())).willReturn(updates);
		Provider<Object> providerOfVersionOption = providerOf(versionOption);
		given(userInputHandler.askUser(any())).willReturn(providerOfVersionOption);
		List<Upgrade> upgrades = upgradeResolver.resolveUpgrades(librariesToUpgrade, libraries);
		assertThat(upgrades.get(0).to().getVersion().getVersion()).isEqualTo(updateVersion);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> Provider<T> providerOf(VersionOption versionOption) {
		Provider provider = mock(Provider.class);
		given(provider.get()).willReturn(versionOption);
		return provider;
	}

}
