/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.compiler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * @author Andy Wilkinson
 */
public final class RepositoryConfigurationFactory {

	private static final RepositoryConfiguration MAVEN_CENTRAL = new RepositoryConfiguration(
			"central", URI.create("http://repo1.maven.org/maven2/"), false);

	private static final RepositoryConfiguration SPRING_MILESTONE = new RepositoryConfiguration(
			"spring-milestone", URI.create("http://repo.spring.io/milestone"), false);

	private static final RepositoryConfiguration SPRING_SNAPSHOT = new RepositoryConfiguration(
			"spring-snapshot", URI.create("http://repo.spring.io/snapshot"), true);

	/**
	 * @return the newly-created default repository configuration
	 */
	public static List<RepositoryConfiguration> createDefaultRepositoryConfiguration() {
		List<RepositoryConfiguration> repositoryConfiguration = new ArrayList<RepositoryConfiguration>();

		repositoryConfiguration.add(MAVEN_CENTRAL);

		if (!Boolean.getBoolean("disableSpringSnapshotRepos")) {
			repositoryConfiguration.add(SPRING_SNAPSHOT);
			repositoryConfiguration.add(SPRING_MILESTONE);
		}

		return repositoryConfiguration;
	}
}
