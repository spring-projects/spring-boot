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
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
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
 * Base class for tasks that upgrade dependencies in a bom.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
public abstract class UpgradeDependencies extends DefaultTask {

	private final BomExtension bom;

	private final boolean movingToSnapshots;

	/**
	 * Constructs a new instance of UpgradeDependencies with the specified BomExtension.
	 * @param bom the BomExtension to be used for upgrading dependencies
	 */
	@Inject
	public UpgradeDependencies(BomExtension bom) {
		this(bom, false);
	}

	/**
	 * Upgrades the dependencies in the specified BOM extension.
	 * @param bom The BOM extension containing the dependencies to be upgraded.
	 * @param movingToSnapshots A boolean indicating whether the dependencies are being
	 * upgraded to snapshots.
	 */
	protected UpgradeDependencies(BomExtension bom, boolean movingToSnapshots) {
		this.bom = bom;
		getThreads().convention(2);
		this.movingToSnapshots = movingToSnapshots;
	}

	/**
	 * Gets the milestone to which dependency upgrade issues should be assigned.
	 * @return The milestone as a String.
	 */
	@Input
	@Option(option = "milestone", description = "Milestone to which dependency upgrade issues should be assigned")
	public abstract Property<String> getMilestone();

	/**
	 * Retrieves the number of threads to use for update resolution.
	 * @return the number of threads to use for update resolution
	 */
	@Input
	@Optional
	@Option(option = "threads", description = "Number of Threads to use for update resolution")
	public abstract Property<Integer> getThreads();

	/**
	 * Retrieves the regular expression that identifies the libraries to upgrade.
	 * @return The regular expression that identifies the libraries to upgrade.
	 */
	@Input
	@Optional
	@Option(option = "libraries", description = "Regular expression that identifies the libraries to upgrade")
	public abstract Property<String> getLibraries();

	/**
	 * Returns the list of repository URIs.
	 * @return the list of repository URIs
	 */
	@Input
	abstract ListProperty<URI> getRepositoryUris();

	/**
	 * Upgrades the dependencies of the project.
	 *
	 * This method performs the following steps: 1. Retrieves the GitHub repository based
	 * on the organization and repository specified in the BOM upgrade configuration. 2.
	 * Verifies the labels associated with the repository. 3. Determines the milestone for
	 * the repository. 4. Resolves the upgrades based on the milestone. 5. Applies the
	 * upgrades to the repository, considering the issue labels and milestone.
	 */
	@TaskAction
	void upgradeDependencies() {
		GitHubRepository repository = createGitHub().getRepository(this.bom.getUpgrade().getGitHub().getOrganization(),
				this.bom.getUpgrade().getGitHub().getRepository());
		List<String> issueLabels = verifyLabels(repository);
		Milestone milestone = determineMilestone(repository);
		List<Upgrade> upgrades = resolveUpgrades(milestone);
		applyUpgrades(repository, issueLabels, milestone, upgrades);
	}

	/**
	 * Applies the specified upgrades to the given GitHub repository.
	 * @param repository The GitHub repository to apply the upgrades to.
	 * @param issueLabels The labels to filter the existing upgrade issues.
	 * @param milestone The milestone to associate the upgrade issues with.
	 * @param upgrades The list of upgrades to apply.
	 */
	private void applyUpgrades(GitHubRepository repository, List<String> issueLabels, Milestone milestone,
			List<Upgrade> upgrades) {
		Path buildFile = getProject().getBuildFile().toPath();
		Path gradleProperties = new File(getProject().getRootProject().getProjectDir(), "gradle.properties").toPath();
		UpgradeApplicator upgradeApplicator = new UpgradeApplicator(buildFile, gradleProperties);
		List<Issue> existingUpgradeIssues = repository.findIssues(issueLabels, milestone);
		System.out.println("Applying upgrades...");
		System.out.println("");
		for (Upgrade upgrade : upgrades) {
			System.out.println(upgrade.getLibrary().getName() + " " + upgrade.getVersion());
			String title = issueTitle(upgrade);
			Issue existingUpgradeIssue = findExistingUpgradeIssue(existingUpgradeIssues, upgrade);
			try {
				Path modified = upgradeApplicator.apply(upgrade);
				int issueNumber = getOrOpenUpgradeIssue(repository, issueLabels, milestone, title,
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

	/**
	 * Retrieves the number of an existing open upgrade issue or opens a new one in the
	 * given GitHub repository.
	 * @param repository The GitHub repository where the issue will be opened.
	 * @param issueLabels The labels to be assigned to the issue.
	 * @param milestone The milestone to be assigned to the issue.
	 * @param title The title of the issue.
	 * @param existingUpgradeIssue The existing upgrade issue, if any.
	 * @return The number of the existing open upgrade issue, if it exists, or the number
	 * of the newly opened issue.
	 */
	private int getOrOpenUpgradeIssue(GitHubRepository repository, List<String> issueLabels, Milestone milestone,
			String title, Issue existingUpgradeIssue) {
		if (existingUpgradeIssue != null && existingUpgradeIssue.getState() == Issue.State.OPEN) {
			return existingUpgradeIssue.getNumber();
		}
		String body = (existingUpgradeIssue != null) ? "Supersedes #" + existingUpgradeIssue.getNumber() : "";
		return repository.openIssue(title, body, issueLabels, milestone);
	}

	/**
	 * Returns the message details for an existing upgrade issue.
	 * @param existingUpgradeIssue the existing upgrade issue to get the message details
	 * for
	 * @return the message details for the existing upgrade issue
	 */
	private String getExistingUpgradeIssueMessageDetails(Issue existingUpgradeIssue) {
		if (existingUpgradeIssue == null) {
			return "";
		}
		if (existingUpgradeIssue.getState() != Issue.State.CLOSED) {
			return " (completes existing upgrade)";
		}
		return " (supersedes #" + existingUpgradeIssue.getNumber() + " " + existingUpgradeIssue.getTitle() + ")";
	}

	/**
	 * Verifies the labels of a GitHub repository.
	 * @param repository the GitHub repository to verify labels for
	 * @return the list of issue labels
	 * @throws InvalidUserDataException if there are unknown labels
	 */
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

	/**
	 * Creates a private GitHub instance using the credentials provided in the
	 * .bomr.properties file.
	 * @return A private GitHub instance
	 * @throws InvalidUserDataException If the .bomr.properties file cannot be loaded from
	 * the user home directory
	 */
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

	/**
	 * Determines the milestone for a given GitHub repository.
	 * @param repository The GitHub repository to determine the milestone for.
	 * @return The matching milestone for the given repository.
	 * @throws InvalidUserDataException If the milestone is not found in the repository.
	 */
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

	/**
	 * Finds an existing upgrade issue in the given list of issues based on the provided
	 * upgrade.
	 * @param existingUpgradeIssues the list of existing upgrade issues to search in
	 * @param upgrade the upgrade to match against
	 * @return the existing upgrade issue if found, null otherwise
	 */
	private Issue findExistingUpgradeIssue(List<Issue> existingUpgradeIssues, Upgrade upgrade) {
		String toMatch = "Upgrade to " + upgrade.getLibrary().getName();
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

	/**
	 * Resolves the upgrades for the given milestone.
	 * @param milestone The milestone for which upgrades need to be resolved.
	 * @return The list of upgrades resolved for the given milestone.
	 */
	@SuppressWarnings("deprecation")
	private List<Upgrade> resolveUpgrades(Milestone milestone) {
		List<Upgrade> upgrades = new InteractiveUpgradeResolver(getServices().get(UserInputHandler.class),
				new MultithreadedLibraryUpdateResolver(getThreads().get(),
						new StandardLibraryUpdateResolver(new MavenMetadataVersionResolver(getRepositoryUris().get()),
								determineUpdatePredicates(milestone))))
			.resolveUpgrades(matchingLibraries(), this.bom.getLibraries());
		return upgrades;
	}

	/**
	 * Determines the update predicates based on the given milestone.
	 * @param milestone the milestone to determine the update predicates for
	 * @return the list of update predicates
	 */
	protected List<BiPredicate<Library, DependencyVersion>> determineUpdatePredicates(Milestone milestone) {
		List<BiPredicate<Library, DependencyVersion>> updatePredicates = new ArrayList<>();
		updatePredicates.add(this::compliesWithUpgradePolicy);
		updatePredicates.add(this::isAnUpgrade);
		updatePredicates.add(this::isNotProhibited);
		return updatePredicates;
	}

	/**
	 * Checks if the given dependency version complies with the upgrade policy of the
	 * library.
	 * @param library the library for which the upgrade policy needs to be checked
	 * @param candidate the dependency version to be tested against the upgrade policy
	 * @return true if the dependency version complies with the upgrade policy, false
	 * otherwise
	 */
	private boolean compliesWithUpgradePolicy(Library library, DependencyVersion candidate) {
		return this.bom.getUpgrade().getPolicy().test(candidate, library.getVersion().getVersion());
	}

	/**
	 * Checks if a given dependency version is an upgrade for a library.
	 * @param library the library to check for upgrade
	 * @param candidate the dependency version to check
	 * @return true if the given dependency version is an upgrade for the library, false
	 * otherwise
	 */
	private boolean isAnUpgrade(Library library, DependencyVersion candidate) {
		return library.getVersion().getVersion().isUpgrade(candidate, this.movingToSnapshots);
	}

	/**
	 * Checks if a given dependency version is not prohibited in a library.
	 * @param library the library to check against
	 * @param candidate the dependency version to check
	 * @return true if the dependency version is not prohibited, false otherwise
	 */
	private boolean isNotProhibited(Library library, DependencyVersion candidate) {
		return library.getProhibitedVersions()
			.stream()
			.noneMatch((prohibited) -> prohibited.isProhibited(candidate.toString()));
	}

	/**
	 * Returns a list of libraries that are eligible for upgrade.
	 * @return a list of libraries that are eligible for upgrade
	 * @throws InvalidUserDataException if there are no libraries to upgrade
	 */
	private List<Library> matchingLibraries() {
		List<Library> matchingLibraries = this.bom.getLibraries().stream().filter(this::eligible).toList();
		if (matchingLibraries.isEmpty()) {
			throw new InvalidUserDataException("No libraries to upgrade");
		}
		return matchingLibraries;
	}

	/**
	 * Checks if a library is eligible based on a given pattern.
	 * @param library the library to check eligibility for
	 * @return true if the library is eligible, false otherwise
	 */
	protected boolean eligible(Library library) {
		String pattern = getLibraries().getOrNull();
		if (pattern == null) {
			return true;
		}
		Predicate<String> libraryPredicate = Pattern.compile(pattern).asPredicate();
		return libraryPredicate.test(library.getName());
	}

	/**
	 * Returns the title of the issue related to the given upgrade.
	 * @param upgrade the upgrade object for which the issue title is to be generated
	 * @return the title of the issue
	 */
	protected abstract String issueTitle(Upgrade upgrade);

	/**
	 * Generates a commit message for the given upgrade and issue number.
	 * @param upgrade The upgrade to generate the commit message for.
	 * @param issueNumber The issue number associated with the upgrade.
	 * @return The commit message as a string.
	 */
	protected abstract String commitMessage(Upgrade upgrade, int issueNumber);

}
