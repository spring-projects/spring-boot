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
import org.springframework.util.StringUtils;

/**
 * Base class for tasks that upgrade dependencies in a bom.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
public abstract class UpgradeDependencies extends DefaultTask {

	private final BomExtension bom;

	@Inject
	public UpgradeDependencies(BomExtension bom) {
		this.bom = bom;
		getThreads().convention(2);
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
	abstract ListProperty<URI> getRepositoryUris();

	@TaskAction
	void upgradeDependencies() {
		GitHubRepository repository = createGitHub().getRepository(this.bom.getUpgrade().getGitHub().getOrganization(),
				this.bom.getUpgrade().getGitHub().getRepository());
		List<String> issueLabels = verifyLabels(repository);
		Milestone milestone = determineMilestone(repository);
		List<Upgrade> upgrades = resolveUpgrades();
		applyUpgrades(repository, issueLabels, milestone, upgrades);
	}

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
				int issueNumber;
				if (existingUpgradeIssue != null && existingUpgradeIssue.getState() == Issue.State.OPEN) {
					issueNumber = existingUpgradeIssue.getNumber();
				}
				else {
					issueNumber = repository.openIssue(title,
							(existingUpgradeIssue != null) ? "Supersedes #" + existingUpgradeIssue.getNumber() : "",
							issueLabels, milestone);
					if (existingUpgradeIssue != null && existingUpgradeIssue.getState() == Issue.State.CLOSED) {
						existingUpgradeIssue.label(Arrays.asList("type: task", "status: superseded"));
					}
				}
				if (existingUpgradeIssue != null) {
					if (existingUpgradeIssue.getState() == Issue.State.CLOSED) {
						System.out.println("   Issue: " + issueNumber + " - " + title + " (supersedes #"
								+ existingUpgradeIssue.getNumber() + " " + existingUpgradeIssue.getTitle() + ")");
					}
					else {
						System.out
							.println("   Issue: " + issueNumber + " - " + title + " (completes existing upgrade)");
					}
				}
				else {
					System.out.println("   Issue: " + issueNumber + " - " + title);
				}
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

	private List<String> verifyLabels(GitHubRepository repository) {
		Set<String> availableLabels = repository.getLabels();
		List<String> issueLabels = this.bom.getUpgrade().getGitHub().getIssueLabels();
		if (!availableLabels.containsAll(issueLabels)) {
			List<String> unknownLabels = new ArrayList<>(issueLabels);
			unknownLabels.removeAll(availableLabels);
			throw new InvalidUserDataException(
					"Unknown label(s): " + StringUtils.collectionToCommaDelimitedString(unknownLabels));
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
		if (!matchingMilestone.isPresent()) {
			throw new InvalidUserDataException("Unknown milestone: " + getMilestone().get());
		}
		return matchingMilestone.get();
	}

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

	@SuppressWarnings("deprecation")
	private List<Upgrade> resolveUpgrades() {
		List<Upgrade> upgrades = new InteractiveUpgradeResolver(getServices().get(UserInputHandler.class),
				new MultithreadedLibraryUpdateResolver(new MavenMetadataVersionResolver(getRepositoryUris().get()),
						this.bom.getUpgrade().getPolicy(), getThreads().get()))
			.resolveUpgrades(matchingLibraries(getLibraries().getOrNull()), this.bom.getLibraries());
		return upgrades;
	}

	private List<Library> matchingLibraries(String pattern) {
		if (pattern == null) {
			return this.bom.getLibraries();
		}
		Predicate<String> libraryPredicate = Pattern.compile(pattern).asPredicate();
		List<Library> matchingLibraries = this.bom.getLibraries()
			.stream()
			.filter((library) -> libraryPredicate.test(library.getName()))
			.toList();
		if (matchingLibraries.isEmpty()) {
			throw new InvalidUserDataException("No libraries matched '" + pattern + "'");
		}
		return matchingLibraries;
	}

	protected abstract String issueTitle(Upgrade upgrade);

	protected abstract String commitMessage(Upgrade upgrade, int issueNumber);

}
