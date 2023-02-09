/*
 * Copyright 2021-2023 the original author or authors.
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

import org.springframework.boot.build.bom.BomExtension;

/**
 * A {@link Task} to move to snapshot dependencies.
 *
 * @author Andy Wilkinson
 */
public abstract class MoveToSnapshots extends UpgradeDependencies {

	private final URI REPOSITORY_URI = URI.create("https://repo.spring.io/snapshot/");

	@Inject
	public MoveToSnapshots(BomExtension bom) {
		super(bom);
		getRepositoryUris().add(this.REPOSITORY_URI);
	}

	@Override
	protected String issueTitle(Upgrade upgrade) {
		String snapshotVersion = upgrade.getVersion().toString();
		String releaseVersion = snapshotVersion.substring(0, snapshotVersion.length() - "-SNAPSHOT".length());
		return "Upgrade to " + upgrade.getLibrary().getName() + " " + releaseVersion;
	}

	@Override
	protected String commitMessage(Upgrade upgrade, int issueNumber) {
		return "Start building against " + upgrade.getLibrary().getName() + " " + releaseVersion(upgrade) + " snapshots"
				+ "\n\nSee gh-" + issueNumber;
	}

	private String releaseVersion(Upgrade upgrade) {
		String snapshotVersion = upgrade.getVersion().toString();
		return snapshotVersion.substring(0, snapshotVersion.length() - "-SNAPSHOT".length());
	}

}
