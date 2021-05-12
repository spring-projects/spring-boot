/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.options.Option;

import org.springframework.boot.build.bom.BomExtension;
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

	private final BomExtension bom;

	private String milestone;

	@Inject
	public UpgradeBom(BomExtension bom) {
		this.bom = bom;
	}

	@Option(option = "milestone", description = "Milestone to which dependency upgrade issues should be assigned")
	public void setMilestone(String milestone) {
		this.milestone = milestone;
	}

	@Input
	public String getMilestone() {
		return this.milestone;
	}

	@TaskAction
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
		List<Upgrade> upgrades = new InteractiveUpgradeResolver(
				new MavenMetadataVersionResolver(Arrays.asList("https://repo1.maven.org/maven2/")),
				this.bom.getUpgrade().getPolicy(), getServices().get(UserInputHandler.class))
						.resolveUpgrades(this.bom.getLibraries());
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
