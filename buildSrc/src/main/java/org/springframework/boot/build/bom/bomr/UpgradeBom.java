/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.tasks.Input;
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
 * {@link Task} to upgrade the libraries managed by a bom.
 *
 * @author Andy Wilkinson
 */
public class UpgradeBom extends DefaultTask {

	private Set<String> repositoryUrls;

	private final BomExtension bom;

	private String milestone;

	private String libraries;

	@Inject
	public UpgradeBom(BomExtension bom) {
		this.bom = bom;
		this.repositoryUrls = new LinkedHashSet<>();
		getProject().getRepositories().withType(MavenArtifactRepository.class, (repository) -> {
			String repositoryUrl = repository.getUrl().toString();
			if (!repositoryUrl.endsWith("snapshot")) {
				this.repositoryUrls.add(repositoryUrl);
			}
		});
	}

	@Option(option = "milestone", description = "Milestone to which dependency upgrade issues should be assigned")
	public void setMilestone(String milestone) {
		this.milestone = milestone;
	}

	@Input
	public String getMilestone() {
		return this.milestone;
	}

	@Option(option = "libraries", description = "Regular expression that identifies the libraries to upgrade")
	public void setLibraries(String libraries) {
		this.libraries = libraries;
	}

	@Input
	@org.gradle.api.tasks.Optional
	public String getLibraries() {
		return this.libraries;
	}

	@TaskAction
	@SuppressWarnings("deprecation")
	void upgradeDependencies() {
		GitHubRepository repository = createGitHub().getRepository(this.bom.getUpgrade().getGitHub().getOrganization(),
				this.bom.getUpgrade().getGitHub().getRepository());
		List<String> availableLabels = repository.getLabels();
		List<String> issueLabels = this.bom.getUpgrade().getGitHub().getIssueLabels();
		if (!availableLabels.containsAll(issueLabels)) {
			List<String> unknownLabels = new ArrayList<>(issueLabels);
			unknownLabels.removeAll(availableLabels);
			throw new InvalidUserDataException(
					"Unknown label(s): " + StringUtils.collectionToCommaDelimitedString(unknownLabels));
		}
		Milestone milestone = determineMilestone(repository);
		List<Issue> existingUpgradeIssues = repository.findIssues(issueLabels, milestone);
		List<Upgrade> upgrades = new InteractiveUpgradeResolver(new MavenMetadataVersionResolver(this.repositoryUrls),
				this.bom.getUpgrade().getPolicy(), getServices().get(UserInputHandler.class))
						.resolveUpgrades(matchingLibraries(this.libraries), this.bom.getLibraries());
		Path buildFile = getProject().getBuildFile().toPath();
		Path gradleProperties = new File(getProject().getRootProject().getProjectDir(), "gradle.properties").toPath();
		UpgradeApplicator upgradeApplicator = new UpgradeApplicator(buildFile, gradleProperties);
		for (Upgrade upgrade : upgrades) {
			String title = "Upgrade to " + upgrade.getLibrary().getName() + " " + upgrade.getVersion();
			Issue existingUpgradeIssue = findExistingUpgradeIssue(existingUpgradeIssues, upgrade);
			if (existingUpgradeIssue != null) {
				System.out.println(title + " (supersedes #" + existingUpgradeIssue.getNumber() + " "
						+ existingUpgradeIssue.getTitle() + ")");
			}
			else {
				System.out.println(title);
			}
			try {
				Path modified = upgradeApplicator.apply(upgrade);
				int issueNumber = repository.openIssue(title,
						(existingUpgradeIssue != null) ? "Supersedes #" + existingUpgradeIssue.getNumber() : "",
						issueLabels, milestone);
				if (existingUpgradeIssue != null) {
					existingUpgradeIssue.label(Arrays.asList("type: task", "status: superseded"));
				}
				if (new ProcessBuilder().command("git", "add", modified.toFile().getAbsolutePath()).start()
						.waitFor() != 0) {
					throw new IllegalStateException("git add failed");
				}
				if (new ProcessBuilder().command("git", "commit", "-m", title + "\n\nCloses gh-" + issueNumber).start()
						.waitFor() != 0) {
					throw new IllegalStateException("git commit failed");
				}
			}
			catch (IOException ex) {
				throw new TaskExecutionException(this, ex);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private List<Library> matchingLibraries(String pattern) {
		if (pattern == null) {
			return this.bom.getLibraries();
		}
		Predicate<String> libraryPredicate = Pattern.compile(pattern).asPredicate();
		List<Library> matchingLibraries = this.bom.getLibraries().stream()
				.filter((library) -> libraryPredicate.test(library.getName())).collect(Collectors.toList());
		if (matchingLibraries.isEmpty()) {
			throw new InvalidUserDataException("No libraries matched '" + pattern + "'");
		}
		return matchingLibraries;
	}

	private Issue findExistingUpgradeIssue(List<Issue> existingUpgradeIssues, Upgrade upgrade) {
		String toMatch = "Upgrade to " + upgrade.getLibrary().getName();
		for (Issue existingUpgradeIssue : existingUpgradeIssues) {
			if (existingUpgradeIssue.getTitle().substring(0, existingUpgradeIssue.getTitle().lastIndexOf(' '))
					.equals(toMatch)) {
				return existingUpgradeIssue;
			}
		}
		return null;
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
		if (this.milestone == null) {
			return null;
		}
		List<Milestone> milestones = repository.getMilestones();
		Optional<Milestone> matchingMilestone = milestones.stream()
				.filter((milestone) -> milestone.getName().equals(this.milestone)).findFirst();
		if (!matchingMilestone.isPresent()) {
			throw new InvalidUserDataException("Unknown milestone: " + this.milestone);
		}
		return matchingMilestone.get();
	}

}
