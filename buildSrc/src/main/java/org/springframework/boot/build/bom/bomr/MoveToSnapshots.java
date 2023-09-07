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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import javax.inject.Inject;

import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.ReleaseSchedule.Release;
import org.springframework.boot.build.bom.bomr.github.Milestone;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * A {@link Task} to move to snapshot dependencies.
 *
 * @author Andy Wilkinson
 */
public abstract class MoveToSnapshots extends UpgradeDependencies {

	private static final Logger log = LoggerFactory.getLogger(MoveToSnapshots.class);

	private final URI REPOSITORY_URI = URI.create("https://repo.spring.io/snapshot/");

	@Inject
	public MoveToSnapshots(BomExtension bom) {
		super(bom, true);
		getRepositoryUris().add(this.REPOSITORY_URI);
	}

	@Override
	@TaskAction
	void upgradeDependencies() {
		super.upgradeDependencies();
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

	@Override
	protected boolean eligible(Library library) {
		return library.isConsiderSnapshots() && super.eligible(library);
	}

	@Override
	protected List<BiPredicate<Library, DependencyVersion>> determineUpdatePredicates(Milestone milestone) {
		ReleaseSchedule releaseSchedule = new ReleaseSchedule();
		Map<String, List<Release>> releases = releaseSchedule.releasesBetween(OffsetDateTime.now(),
				milestone.getDueOn());
		List<BiPredicate<Library, DependencyVersion>> predicates = super.determineUpdatePredicates(milestone);
		predicates.add((library, candidate) -> {
			List<Release> releasesForLibrary = releases.get(library.getCalendarName());
			if (releasesForLibrary != null) {
				for (Release release : releasesForLibrary) {
					if (candidate.isSnapshotFor(release.getVersion())) {
						return true;
					}
				}
			}
			if (log.isInfoEnabled()) {
				log.info("Ignoring " + candidate + ". No release of " + library.getName() + " scheduled before "
						+ milestone.getDueOn());
			}
			return false;
		});
		return predicates;
	}

}
