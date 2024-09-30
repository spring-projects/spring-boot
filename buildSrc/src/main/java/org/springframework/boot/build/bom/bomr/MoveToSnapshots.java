/*
 * Copyright 2021-2024 the original author or authors.
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import javax.inject.Inject;

import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.ReleaseSchedule.Release;
import org.springframework.boot.build.bom.bomr.github.Milestone;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.boot.build.properties.BuildProperties;

/**
 * A {@link Task} to move to snapshot dependencies.
 *
 * @author Andy Wilkinson
 */
public abstract class MoveToSnapshots extends UpgradeDependencies {

	private static final Logger logger = LoggerFactory.getLogger(MoveToSnapshots.class);

	@Inject
	public MoveToSnapshots(BomExtension bom) {
		super(bom, true);
		getProject().getRepositories().withType(MavenArtifactRepository.class, (repository) -> {
			String name = repository.getName();
			if (name.startsWith("spring-") && name.endsWith("-snapshot")) {
				getRepositoryNames().add(name);
			}
		});
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
		return switch (BuildProperties.get(getProject()).buildType()) {
			case OPEN_SOURCE -> determineOpenSourceUpdatePredicates(milestone);
			case COMMERCIAL -> super.determineUpdatePredicates(milestone);
		};
	}

	private List<BiPredicate<Library, DependencyVersion>> determineOpenSourceUpdatePredicates(Milestone milestone) {
		Map<String, List<Release>> scheduledReleases = getScheduledOpenSourceReleases(milestone);
		List<BiPredicate<Library, DependencyVersion>> predicates = super.determineUpdatePredicates(milestone);
		predicates.add((library, candidate) -> {
			List<Release> releases = scheduledReleases.get(library.getCalendarName());
			boolean match = (releases != null)
					&& releases.stream().anyMatch((release) -> candidate.isSnapshotFor(release.getVersion()));
			if (logger.isInfoEnabled() && !match) {
				logger.info("Ignoring {}. No release of {} scheduled before {}", candidate, library.getName(),
						milestone.getDueOn());
			}
			return match;
		});
		return predicates;
	}

	private Map<String, List<Release>> getScheduledOpenSourceReleases(Milestone milestone) {
		ReleaseSchedule releaseSchedule = new ReleaseSchedule();
		return releaseSchedule.releasesBetween(OffsetDateTime.now(), milestone.getDueOn());
	}

}
