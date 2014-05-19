package org.springframework.boot.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;

/**
 * An {@link org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter
 * ArtifactsFilter} that filters by matching groupId.
 *
 * Preferred over the {@link org.apache.maven.shared.artifact.filter.collection.GroupIdFilter} due
 * to that classes use of {@link String#startsWith} to match on prefix.
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

	protected String getArtifactFeature(Artifact artifact) {
		return artifact.getGroupId();
	}
}
