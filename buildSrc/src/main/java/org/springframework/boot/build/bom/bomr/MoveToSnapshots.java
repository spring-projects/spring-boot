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

	/**
	 * Constructs a new MoveToSnapshots object with the specified BomExtension.
	 * @param bom the BomExtension to be used
	 */
	@Inject
	public MoveToSnapshots(BomExtension bom) {
		super(bom, true);
		getRepositoryUris().add(this.REPOSITORY_URI);
	}

	/**
	 * Upgrades the dependencies of the MoveToSnapshots class. This method overrides the
	 * upgradeDependencies() method from the superclass. It calls the superclass's
	 * upgradeDependencies() method using the super keyword.
	 */
	@Override
	@TaskAction
	void upgradeDependencies() {
		super.upgradeDependencies();
	}

	/**
	 * Generates the issue title for an upgrade.
	 * @param upgrade the upgrade object containing the library and version information
	 * @return the issue title in the format "Upgrade to [library name] [release version]"
	 */
	@Override
	protected String issueTitle(Upgrade upgrade) {
		String snapshotVersion = upgrade.getVersion().toString();
		String releaseVersion = snapshotVersion.substring(0, snapshotVersion.length() - "-SNAPSHOT".length());
		return "Upgrade to " + upgrade.getLibrary().getName() + " " + releaseVersion;
	}

	/**
	 * Generates a commit message for starting the build against a specific library
	 * upgrade and issue number.
	 * @param upgrade the upgrade object containing information about the library upgrade
	 * @param issueNumber the issue number associated with the upgrade
	 * @return the commit message
	 */
	@Override
	protected String commitMessage(Upgrade upgrade, int issueNumber) {
		return "Start building against " + upgrade.getLibrary().getName() + " " + releaseVersion(upgrade) + " snapshots"
				+ "\n\nSee gh-" + issueNumber;
	}

	/**
	 * Returns the release version of the given upgrade by removing the "-SNAPSHOT"
	 * suffix.
	 * @param upgrade the upgrade object containing the version information
	 * @return the release version of the upgrade
	 */
	private String releaseVersion(Upgrade upgrade) {
		String snapshotVersion = upgrade.getVersion().toString();
		return snapshotVersion.substring(0, snapshotVersion.length() - "-SNAPSHOT".length());
	}

	/**
	 * Checks if the given library is eligible for moving to snapshots.
	 * @param library the library to check
	 * @return true if the library is eligible for moving to snapshots, false otherwise
	 */
	@Override
	protected boolean eligible(Library library) {
		return library.isConsiderSnapshots() && super.eligible(library);
	}

	/**
	 * Determines the update predicates for the given milestone.
	 * @param milestone The milestone for which to determine the update predicates.
	 * @return The list of update predicates.
	 */
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
