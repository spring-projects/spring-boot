/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.boot.build.repository;

import org.springframework.boot.build.properties.BuildType;

/**
 * Enumeration of repositories defined in the order that they should be used.
 *
 * @author Phillip Webb
 */
public enum SpringRepository {

	/**
	 * Repository for commercial releases.
	 */
	COMMERCIAL_RELEASE("spring-commerical-release", BuildType.COMMERCIAL, RepositoryType.RELEASE,
			RepositoryUrl.commercial("/spring-enterprise-maven-prod-local")),

	/**
	 * Repository for open source milestones.
	 */
	OSS_MILESTONE("spring-oss-milestone", BuildType.OPEN_SOURCE, RepositoryType.MILESTONE,
			RepositoryUrl.openSource("/milestone")),

	/**
	 * Repository for commercial snapshots.
	 */
	COMMERCIAL_SNAPSHOT("spring-commerical-snapshot", BuildType.COMMERCIAL, RepositoryType.SNAPSHOT,
			RepositoryUrl.commercial("/spring-enterprise-maven-dev-local")),

	/**
	 * Repository for open source snapshots.
	 */
	OSS_SNAPSHOT("spring-oss-snapshot", BuildType.OPEN_SOURCE, RepositoryType.SNAPSHOT,
			RepositoryUrl.openSource("/snapshot"));

	private final String name;

	private final BuildType buildType;

	private final RepositoryType repositoryType;

	private final String url;

	SpringRepository(String name, BuildType buildType, RepositoryType repositoryType, String url) {
		this.name = name;
		this.buildType = buildType;
		this.repositoryType = repositoryType;
		this.url = url;
	}

	public String getName() {
		return this.name;
	}

	public BuildType getBuildType() {
		return this.buildType;
	}

	public RepositoryType getRepositoryType() {
		return this.repositoryType;
	}

	public String getUrl() {
		return this.url;
	}

	/**
	 * Repository types.
	 */
	public enum RepositoryType {

		/**
		 * Repository containing release artifacts.
		 */
		RELEASE,

		/**
		 * Repository containing milestone artifacts.
		 */
		MILESTONE,

		/**
		 * Repository containing snapshot artifacts.
		 */
		SNAPSHOT

	}

}
