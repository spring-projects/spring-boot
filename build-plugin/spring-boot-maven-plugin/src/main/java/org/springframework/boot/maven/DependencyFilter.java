/*
 * Copyright 2012-present the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * Base class for {@link ArtifactsFilter} based on a {@link FilterableDependency} list.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.2.0
 */
public abstract class DependencyFilter extends AbstractArtifactsFilter {

	private final List<? extends FilterableDependency> filters;

	/**
	 * Create a new instance with the list of {@link FilterableDependency} instance(s) to
	 * use.
	 * @param dependencies the source dependencies
	 */
	public DependencyFilter(List<? extends FilterableDependency> dependencies) {
		this.filters = dependencies;
	}

	@Override
	public Set<Artifact> filter(Set<Artifact> artifacts) throws ArtifactFilterException {
		Set<Artifact> result = new HashSet<>();
		for (Artifact artifact : artifacts) {
			if (!filter(artifact)) {
				result.add(artifact);
			}
		}
		return result;
	}

	protected abstract boolean filter(Artifact artifact);

	/**
	 * Check if the specified {@link org.apache.maven.artifact.Artifact} matches the
	 * specified {@link org.springframework.boot.maven.FilterableDependency}. Returns
	 * {@code true} if it should be excluded
	 * @param artifact the Maven {@link Artifact}
	 * @param dependency the {@link FilterableDependency}
	 * @return {@code true} if the artifact matches the dependency
	 */
	protected final boolean equals(Artifact artifact, FilterableDependency dependency) {
		if (!dependency.getGroupId().equals(artifact.getGroupId())) {
			return false;
		}
		if (!dependency.getArtifactId().equals(artifact.getArtifactId())) {
			return false;
		}
		return (dependency.getClassifier() == null
				|| artifact.getClassifier() != null && dependency.getClassifier().equals(artifact.getClassifier()));
	}

	protected final List<? extends FilterableDependency> getFilters() {
		return this.filters;
	}

	/**
	 * Return a new {@link DependencyFilter} the excludes artifacts based on the given
	 * predicate.
	 * @param filter the predicate used to filter the artifacts.
	 * @return a new {@link DependencyFilter} instance
	 * @since 3.5.7
	 */
	public static DependencyFilter exclude(Predicate<Artifact> filter) {
		return new DependencyFilter(Collections.emptyList()) {

			@Override
			protected boolean filter(Artifact artifact) {
				return filter.test(artifact);
			}

		};
	}

}
