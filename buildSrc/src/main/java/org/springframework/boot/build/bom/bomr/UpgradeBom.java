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

import javax.inject.Inject;

import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactRepositoryContainer;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.properties.BuildProperties;
import org.springframework.boot.build.repository.SpringRepository;

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
		switch (BuildProperties.get(this).buildType()) {
			case OPEN_SOURCE -> addOpenSourceRepositories();
			case COMMERCIAL -> addCommercialRepositories();
		}
	}

	private void addOpenSourceRepositories() {
		getProject().getRepositories().withType(MavenArtifactRepository.class, (repository) -> {
			if (!isSnaphotRepository(repository)) {
				getRepositoryNames().add(repository.getName());
			}
		});
	}

	private void addCommercialRepositories() {
		getRepositoryNames().addAll(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME,
				SpringRepository.COMMERCIAL_RELEASE.getName());
	}

	private boolean isSnaphotRepository(MavenArtifactRepository repository) {
		return repository.getUrl().toString().endsWith("snapshot");
	}

	@Override
	protected String issueTitle(Upgrade upgrade) {
		return "Upgrade to " + upgrade.getLibrary().getName() + " " + upgrade.getVersion();
	}

	@Override
	protected String commitMessage(Upgrade upgrade, int issueNumber) {
		return issueTitle(upgrade) + "\n\nCloses gh-" + issueNumber;
	}

}
