/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * A model for a dependency to include or exclude.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 3.1.11
 */
public abstract class FilterableDependency {

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

	String getGroupId() {
		return this.groupId;
	}

	void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	String getArtifactId() {
		return this.artifactId;
	}

	void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	String getClassifier() {
		return this.classifier;
	}

	void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	/**
	 * Configures the include or exclude using a user-provided property in the form
	 * {@code groupId:artifactId} or {@code groupId:artifactId:classifier}.
	 * @param property the user-provided property
	 */
	public void set(String property) {
		String[] parts = property.split(":");
		Assert.isTrue(parts.length == 2 || parts.length == 3, getClass().getSimpleName()
				+ " 'property' must be in the form groupId:artifactId or groupId:artifactId:classifier");
		setGroupId(parts[0]);
		setArtifactId(parts[1]);
		if (parts.length == 3) {
			setClassifier(parts[2]);
		}
	}

}
