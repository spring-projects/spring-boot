/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;

/**
 * An {@link org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter
 * ArtifactsFilter} that filters out any artifact matching a configurable list of
 * {@link Exclude} instances.
 *
 * @author Stephane Nicoll
 * @since 1.1
 */
public class ExcludeFilter extends AbstractArtifactsFilter {

	private final List<Exclude> excludes;

	/**
	 * Create a new instance with the list of {@link Exclude} instance(s) to use.
	 */
	public ExcludeFilter(List<Exclude> excludes) {
		this.excludes = excludes;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Set filter(Set artifacts) throws ArtifactFilterException {
		Set<Artifact> result = new HashSet<Artifact>();
		for (Object a : artifacts) {
			Artifact artifact = (Artifact) a;
			if (!matchExclude(artifact)) {
				result.add(artifact);
			}
		}
		return result;
	}

	/**
	 * Check if the specified {@link Artifact} matches one of the known excludes. Returns
	 * {@code true} if it should be excluded
	 */
	private boolean matchExclude(Artifact artifact) {
		for (Exclude exclude : this.excludes) {
			if (match(artifact, exclude)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the specified {@link Artifact} matches the specified {@link Exclude}.
	 * Returns {@code true} if it should be excluded
	 */
	private boolean match(Artifact artifact, Exclude exclude) {
		if (!exclude.getGroupId().equals(artifact.getGroupId())) {
			return false;
		}
		if (!exclude.getArtifactId().equals(artifact.getArtifactId())) {
			return false;
		}
		return (exclude.getClassifier() == null || artifact.getClassifier() != null
				&& exclude.getClassifier().equals(artifact.getClassifier()));

	}

}
