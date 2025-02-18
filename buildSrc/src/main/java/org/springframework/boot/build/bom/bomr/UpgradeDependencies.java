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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.options.Option;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.github.GitHub;
import org.springframework.boot.build.bom.bomr.github.GitHubRepository;
import org.springframework.boot.build.bom.bomr.github.Issue;
import org.springframework.boot.build.bom.bomr.github.Milestone;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.util.StringUtils;

/**
 * Base class for tasks that upgrade dependencies in a BOM.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
public abstract class UpgradeDependencies extends DefaultTask {

	private final BomExtension bom;

	private final boolean movingToSnapshots;

	private final UpgradeApplicator upgradeApplicator;

	private final RepositoryHandler repositories;

	@Inject
	public UpgradeDependencies(BomExtension bom) {
		this(bom, false);
	}

	protected UpgradeDependencies(BomExtension bom, boolean movingToSnapshots) {
		this.bom = bom;
		getThreads().convention(2);
		this.movingToSnapshots = movingToSnapshots;
		this.upgradeApplicator = new UpgradeApplicator(getProject().getBuildFile().toPath(),
				new File(getProject().getRootProject().getProjectDir(), "gradle.properties").toPath());
		this.repositories = getProject().getRepositories();
	}

	@Input
	@Option(option = "milestone", description = "Milestone to which dependency upgrade issues should be assigned")
	public abstract Property<String> getMilestone();

	@Input
	@Optional
	@Option(option = "threads", description = "Number of Threads to use for update resolution")
	public abstract Property<Integer> getThreads();

	@Input
	@Optional
	@Option(option = "libraries", description = "Regular expression that identifies the libraries to upgrade")
	public abstract Property<String> getLibraries();

	@Input
	abstract ListProperty<String> getRepositoryNames();

	@TaskAction
	void upgradeDependencies() {
		GitHubRepository repository = createGitHub().getRepository(this.bom.getUpgrade().getGitHub().getOrganization(),
				this.bom.getUpgrade().getGitHub().getRepository());
		List<String> issueLabels = verifyLabels(repository);
		Milestone milestone = determineMilestone(repository);
		List<Upgrade> upgrades = resolveUpgrades(milestone);
		applyUpgrades(repository, issueLabels, milestone, upgrades);
	}

	private void applyUpgrades(GitHubRepository repository, List<String> issueLabels, Milestone milestone,
			List<Upgrade> upgrades) {
		List<Issue> existingUpgradeIssues = repository.findIssues(issueLabels, milestone);
		System.out.println("Applying upgrades...");
		System.out.println("");
		for (Upgrade upgrade : upgrades) {
			System.out.println(upgrade.to().getNameAndVersion());
			Issue existingUpgradeIssue = findExistingUpgradeIssue(existingUpgradeIssues, upgrade);
			try {
				Path modified = this.upgradeApplicator.apply(upgrade);
				String title = issueTitle(upgrade);
				String body = issueBody(upgrade, existingUpgradeIssue);
				int issueNumber = getOrOpenUpgradeIssue(repository, issueLabels, milestone, title, body,
						existingUpgradeIssue);
				if (existingUpgradeIssue != null && existingUpgradeIssue.getState() == Issue.State.CLOSED) {
					existingUpgradeIssue.label(Arrays.asList("type: task", "status: superseded"));
				}
				System.out.println("   Issue: " + issueNumber + " - " + title
						+ getExistingUpgradeIssueMessageDetails(existingUpgradeIssue));
				if (new ProcessBuilder().command("git", "add", modified.toFile().getAbsolutePath())
					.start()
					.waitFor() != 0) {
					throw new IllegalStateException("git add failed");
				}
				String commitMessage = commitMessage(upgrade, issueNumber);
				if (new ProcessBuilder().command("git", "commit", "-m", commitMessage).start().waitFor() != 0) {
					throw new IllegalStateException("git commit failed");
				}
				System.out.println("  Commit: " + commitMessage.substring(0, commitMessage.indexOf('\n')));
			}
			catch (IOException ex) {
				throw new TaskExecutionException(this, ex);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private int getOrOpenUpgradeIssue(GitHubRepository repository, List<String> issueLabels, Milestone milestone,
			String title, String body, Issue existingUpgradeIssue) {
		if (existingUpgradeIssue != null && existingUpgradeIssue.getState() == Issue.State.OPEN) {
			return existingUpgradeIssue.getNumber();
		}
		return repository.openIssue(title, body, issueLabels, milestone);
	}

	private String getExistingUpgradeIssueMessageDetails(Issue existingUpgradeIssue) {
		if (existingUpgradeIssue == null) {
			return "";
		}
		if (existingUpgradeIssue.getState() != Issue.State.CLOSED) {
			return " (completes existing upgrade)";
		}
		return " (supersedes #" + existingUpgradeIssue.getNumber() + " " + existingUpgradeIssue.getTitle() + ")";
	}

	private List<String> verifyLabels(GitHubRepository repository) {
		Set<String> availableLabels = repository.getLabels();
		List<String> issueLabels = this.bom.getUpgrade().getGitHub().getIssueLabels();
		if (!availableLabels.containsAll(issueLabels)) {
			List<String> unknownLabels = new ArrayList<>(issueLabels);
			unknownLabels.removeAll(availableLabels);
			String suffix = (unknownLabels.size() == 1) ? "" : "s";
			throw new InvalidUserDataException(
					"Unknown label" + suffix + ": " + StringUtils.collectionToCommaDelimitedString(unknownLabels));
		}
		return issueLabels;
	}

	private GitHub createGitHub() {
		Properties bomrProperties = new Properties();
		try (Reader reader = new FileReader(new File(System.getProperty("user.home"), ".bomr.properties"))) {
			bomrProperties.load(reader);
			String username = bomrProperties.getProperty("bomr.github.username");
			String password = bomrProperties.getProperty("bomr.github.password");
			return GitHub.withCredentials(username, password);
		}
		catch (IOException ex) {
			throw new InvalidUserDataException("Failed to load .bomr.properties from user home", ex);
		}
	}

	private Milestone determineMilestone(GitHubRepository repository) {
		List<Milestone> milestones = repository.getMilestones();
		java.util.Optional<Milestone> matchingMilestone = milestones.stream()
			.filter((milestone) -> milestone.getName().equals(getMilestone().get()))
			.findFirst();
		if (matchingMilestone.isEmpty()) {
			throw new InvalidUserDataException("Unknown milestone: " + getMilestone().get());
		}
		return matchingMilestone.get();
	}

	private Issue findExistingUpgradeIssue(List<Issue> existingUpgradeIssues, Upgrade upgrade) {
		String toMatch = "Upgrade to " + upgrade.toRelease().getName();
		for (Issue existingUpgradeIssue : existingUpgradeIssues) {
			String title = existingUpgradeIssue.getTitle();
			int lastSpaceIndex = title.lastIndexOf(' ');
			if (lastSpaceIndex > -1) {
				title = title.substring(0, lastSpaceIndex);
			}
			if (title.equals(toMatch)) {
				return existingUpgradeIssue;
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private List<Upgrade> resolveUpgrades(Milestone milestone) {
		InteractiveUpgradeResolver upgradeResolver = new InteractiveUpgradeResolver(
				getServices().get(UserInputHandler.class), getLibraryUpdateResolver(milestone));
		return upgradeResolver.resolveUpgrades(matchingLibraries(), this.bom.getLibraries());
	}

	private LibraryUpdateResolver getLibraryUpdateResolver(Milestone milestone) {
		VersionResolver versionResolver = new MavenMetadataVersionResolver(getRepositories());
		LibraryUpdateResolver libraryResolver = new StandardLibraryUpdateResolver(versionResolver,
				createVersionOptionResolver(milestone));
		return new MultithreadedLibraryUpdateResolver(getThreads().get(), libraryResolver);
	}

	private Collection<MavenArtifactRepository> getRepositories() {
		return getRepositoryNames().map(this::asRepositories).get();
	}

	private List<MavenArtifactRepository> asRepositories(List<String> repositoryNames) {
		return repositoryNames.stream()
			.map(this.repositories::getByName)
			.map(MavenArtifactRepository.class::cast)
			.toList();
	}

	protected BiFunction<Library, DependencyVersion, VersionOption> createVersionOptionResolver(Milestone milestone) {
		List<BiPredicate<Library, DependencyVersion>> updatePredicates = new ArrayList<>();
		updatePredicates.add(this::compliesWithUpgradePolicy);
		updatePredicates.add(this::isAnUpgrade);
		updatePredicates.add(this::isNotProhibited);
		return (library, dependencyVersion) -> {
			if (this.compliesWithUpgradePolicy(library, dependencyVersion)
					&& this.isAnUpgrade(library, dependencyVersion)
					&& this.isNotProhibited(library, dependencyVersion)) {
				return new VersionOption.ResolvedVersionOption(dependencyVersion, Collections.emptyList());
			}
			return null;
		};
	}

	private boolean compliesWithUpgradePolicy(Library library, DependencyVersion candidate) {
		return this.bom.getUpgrade().getPolicy().test(candidate, library.getVersion().getVersion());
	}

	private boolean isAnUpgrade(Library library, DependencyVersion candidate) {
		return library.getVersion().getVersion().isUpgrade(candidate, this.movingToSnapshots);
	}

	private boolean isNotProhibited(Library library, DependencyVersion candidate) {
		return library.getProhibitedVersions()
			.stream()
			.noneMatch((prohibited) -> prohibited.isProhibited(candidate.toString()));
	}

	private List<Library> matchingLibraries() {
		List<Library> matchingLibraries = this.bom.getLibraries().stream().filter(this::eligible).toList();
		if (matchingLibraries.isEmpty()) {
			throw new InvalidUserDataException("No libraries to upgrade");
		}
		return matchingLibraries;
	}

	protected boolean eligible(Library library) {
		String pattern = getLibraries().getOrNull();
		if (pattern == null) {
			return true;
		}
		Predicate<String> libraryPredicate = Pattern.compile(pattern).asPredicate();
		return libraryPredicate.test(library.getName());
	}

	protected abstract String commitMessage(Upgrade upgrade, int issueNumber);

	protected String issueTitle(Upgrade upgrade) {
		return "Upgrade to " + upgrade.toRelease().getNameAndVersion();
	}

	protected String issueBody(Upgrade upgrade, Issue existingUpgrade) {
		String description = upgrade.toRelease().getNameAndVersion();
		String releaseNotesLink = upgrade.toRelease().getLinkUrl("releaseNotes");
		String body = (releaseNotesLink != null) ? "Upgrade to [%s](%s).".formatted(description, releaseNotesLink)
				: "Upgrade to %s.".formatted(description);
		if (existingUpgrade != null) {
			body += "\n\nSupersedes #" + existingUpgrade.getNumber();
		}
		return body;
	}

}
