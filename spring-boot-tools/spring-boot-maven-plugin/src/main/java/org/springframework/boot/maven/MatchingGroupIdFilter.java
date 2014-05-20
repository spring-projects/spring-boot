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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;

/**
 * An {@link org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter
 * ArtifactsFilter} that filters by matching groupId.
 * 
 * Preferred over the
 * {@link org.apache.maven.shared.artifact.filter.collection.GroupIdFilter} due to that
 * classes use of {@link String#startsWith} to match on prefix.
 * 
 * @author Mark Ingram
 * @since 1.1
 */
public class MatchingGroupIdFilter extends AbstractArtifactFeatureFilter {

	/**
	 * Create a new instance with the CSV groupId values that should be excluded.
	 */
	public MatchingGroupIdFilter(String exclude) {
		super("", exclude);
	}

	@Override
	protected String getArtifactFeature(Artifact artifact) {
		return artifact.getGroupId();
	}

}
