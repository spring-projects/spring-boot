/*
 * Copyright 2012-2020 the original author or authors.
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
