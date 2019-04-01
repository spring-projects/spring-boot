/*
 * Copyright 2012-2017 the original author or authors.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;

/**
 * An {@link org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter
 * ArtifactsFilter} that filters by matching version.
 *
 * Use baseVersion in order to be compatible with SNAPSHOT jars.
 *
 * @author <a href="mailto:daonan.zhan@gmail.com">Joshua Jeme</a>
 * @since 2.2.0
 */
public class MatchingBaseVersionFilter extends AbstractArtifactFeatureFilter {

	/**
	 * Create a new instance with the CSV groupId values that should be excluded.
	 * @param exclude the version values to exclude
	 */
	public MatchingBaseVersionFilter(String exclude) {
		super("", exclude);
	}

	@Override
	protected String getArtifactFeature(Artifact artifact) {
		return artifact.getBaseVersion();
	}

}
