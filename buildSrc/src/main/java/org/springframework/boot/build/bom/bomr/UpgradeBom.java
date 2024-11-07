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
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactRepositoryContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library.LibraryVersion;
import org.springframework.boot.build.bom.Library.Link;
import org.springframework.boot.build.bom.bomr.github.Issue;
import org.springframework.boot.build.properties.BuildProperties;

/**
 * {@link Task} to upgrade the libraries managed by a bom.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
public abstract class UpgradeBom extends UpgradeDependencies {

	@Inject
	public UpgradeBom(BomExtension bom) {
		super(bom);
		switch (BuildProperties.get(getProject()).buildType()) {
			case OPEN_SOURCE -> addOpenSourceRepositories(getProject().getRepositories());
			case COMMERCIAL -> addCommercialRepositories();
		}
	}

	private void addOpenSourceRepositories(RepositoryHandler repositories) {
		getRepositoryNames().add(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME);
		repositories.withType(MavenArtifactRepository.class, (repository) -> {
			String name = repository.getName();
			if (name.startsWith("spring-") && !name.endsWith("-snapshot")) {
				getRepositoryNames().add(name);
			}
		});
	}

	private void addCommercialRepositories() {
		getRepositoryNames().addAll(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME,
				"spring-commercial-release");
	}

	@Override
	protected String issueTitle(Upgrade upgrade) {
		return "Upgrade to " + upgrade.getLibrary().getName() + " " + upgrade.getVersion();
	}

	@Override
	protected String commitMessage(Upgrade upgrade, int issueNumber) {
		return issueTitle(upgrade) + "\n\nCloses gh-" + issueNumber;
	}

	@Override
	protected String issueBody(Upgrade upgrade, Issue existingUpgrade) {
		LibraryVersion upgradeVersion = new LibraryVersion(upgrade.getVersion());
		String releaseNotesLink = getReleaseNotesLink(upgrade, upgradeVersion);
		List<String> lines = new ArrayList<>();
		String description = upgrade.getLibrary().getName() + " " + upgradeVersion;
		if (releaseNotesLink != null) {
			lines.add("Upgrade to [%s](%s).".formatted(description, releaseNotesLink));
		}
		else {
			lines.add("Upgrade to %s.".formatted(description));
		}
		if (existingUpgrade != null) {
			lines.add("Supersedes #" + existingUpgrade.getNumber());
		}
		return String.join("\\r\\n\\r\\n", lines);
	}

	private String getReleaseNotesLink(Upgrade upgrade, LibraryVersion upgradeVersion) {
		Link releaseNotesLink = upgrade.getLibrary().getLink("releaseNotes");
		return releaseNotesLink.url(upgradeVersion);
	}

}
