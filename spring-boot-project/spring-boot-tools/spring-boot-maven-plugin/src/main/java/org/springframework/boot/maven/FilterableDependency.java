/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.maven;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * A model for a dependency to include or exclude.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 */
abstract class FilterableDependency {

	/**
	 * The groupId of the artifact to exclude.
	 */
	@Parameter(required = true)
	private String groupId;

	/**
	 * The artifactId of the artifact to exclude.
	 */
	@Parameter(required = true)
	private String artifactId;

	/**
	 * The classifier of the artifact to exclude.
	 */
	@Parameter
	private String classifier;

	/**
	 * Returns the group ID of the FilterableDependency.
	 * @return the group ID of the FilterableDependency
	 */
	String getGroupId() {
		return this.groupId;
	}

	/**
	 * Sets the group ID for the FilterableDependency.
	 * @param groupId the group ID to be set
	 */
	void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * Returns the artifact ID of the FilterableDependency.
	 * @return the artifact ID of the FilterableDependency
	 */
	String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Sets the artifact ID of the FilterableDependency.
	 * @param artifactId the artifact ID to be set
	 */
	void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * Returns the classifier of the FilterableDependency.
	 * @return the classifier of the FilterableDependency
	 */
	String getClassifier() {
		return this.classifier;
	}

	/**
	 * Sets the classifier for the FilterableDependency.
	 * @param classifier the classifier to be set
	 */
	void setClassifier(String classifier) {
		this.classifier = classifier;
	}

}
