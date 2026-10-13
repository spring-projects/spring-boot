/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

/**
 * An {@link ArtifactsFilter} that filters out any artifact not matching an
 * {@link Include}.
 *
 * @author David Turanski
 * @since 1.2.0
 */
public class IncludeFilter extends DependencyFilter {

	public IncludeFilter(List<Include> includes) {
		super(includes);
	}

	@Override
	protected boolean filter(Artifact artifact) {
		for (FilterableDependency dependency : getFilters()) {
			if (equals(artifact, dependency)) {
				return false;
			}
		}
		return true;
	}

}
