/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.github;

import java.util.List;
import java.util.Set;

/**
 * Minimal API for interacting with a GitHub repository.
 *
 * @author Andy Wilkinson
 */
public interface GitHubRepository {

	/**
	 * Opens a new issue with the given title. The given {@code labels} will be applied to
	 * the issue and it will be assigned to the given {@code milestone}.
	 * @param title the title of the issue
	 * @param body the body of the issue
	 * @param labels the labels to apply to the issue
	 * @param milestone the milestone to assign the issue to
	 * @return the number of the new issue
	 */
	int openIssue(String title, String body, List<String> labels, Milestone milestone);

	/**
	 * Returns the labels in the repository.
	 * @return the labels
	 */
	Set<String> getLabels();

	/**
	 * Returns the milestones in the repository.
	 * @return the milestones
	 */
	List<Milestone> getMilestones();

	/**
	 * Finds issues that have the given {@code labels} and are assigned to the given
	 * {@code milestone}.
	 * @param labels issue labels
	 * @param milestone assigned milestone
	 * @return the matching issues
	 */
	List<Issue> findIssues(List<String> labels, Milestone milestone);

	/**
	 * Returns the content of the repository at the given {@code path} and {@code ref}.
	 * @param path the path in the repository
	 * @param ref the ref in the repository, typically a branch name
	 * @return the content, possibly {@code null}
	 */
	String getContent(String path, String ref);

}
