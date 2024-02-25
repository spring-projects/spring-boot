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

import java.net.URI;

import javax.inject.Inject;

import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import org.springframework.boot.build.bom.BomExtension;

/**
 * {@link Task} to upgrade the libraries managed by a bom.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
public abstract class UpgradeBom extends UpgradeDependencies {

	/**
     * Constructs a new UpgradeBom object with the specified BomExtension.
     * 
     * @param bom the BomExtension to be used for the upgrade
     */
    @Inject
	public UpgradeBom(BomExtension bom) {
		super(bom);
		getProject().getRepositories().withType(MavenArtifactRepository.class, (repository) -> {
			URI repositoryUrl = repository.getUrl();
			if (!repositoryUrl.toString().endsWith("snapshot")) {
				getRepositoryUris().add(repositoryUrl);
			}
		});
	}

	/**
     * Generates the issue title for an upgrade.
     * 
     * @param upgrade the upgrade object containing the library and version information
     * @return the issue title in the format "Upgrade to [library name] [version]"
     */
    @Override
	protected String issueTitle(Upgrade upgrade) {
		return "Upgrade to " + upgrade.getLibrary().getName() + " " + upgrade.getVersion();
	}

	/**
     * Generates the commit message for a given upgrade and issue number.
     * 
     * @param upgrade The upgrade object.
     * @param issueNumber The issue number.
     * @return The commit message.
     */
    @Override
	protected String commitMessage(Upgrade upgrade, int issueNumber) {
		return issueTitle(upgrade) + "\n\nCloses gh-" + issueNumber;
	}

}
