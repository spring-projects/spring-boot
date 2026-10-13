/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.github;

/**
 * Minimal API for interacting with GitHub.
 *
 * @author Andy Wilkinson
 */
public interface GitHub {

	/**
	 * Returns a {@link GitHubRepository} with the given {@code name} in the given
	 * {@code organization}.
	 * @param organization the organization
	 * @param name the name of the repository
	 * @return the repository
	 */
	GitHubRepository getRepository(String organization, String name);

	/**
	 * Creates a new {@code GitHub} that will authenticate with given {@code username} and
	 * {@code password}.
	 * @param username username for authentication
	 * @param password password for authentication
	 * @return the new {@code GitHub} instance
	 */
	static GitHub withCredentials(String username, String password) {
		return new StandardGitHub(username, password);
	}

}
