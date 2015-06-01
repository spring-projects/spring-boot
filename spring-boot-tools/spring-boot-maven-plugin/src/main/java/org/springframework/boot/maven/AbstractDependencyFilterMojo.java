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

import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

/**
 * A base mojo filtering the dependencies of the project.
 *
 * @author Stephane Nicoll
 * @since 1.1
 */
public abstract class AbstractDependencyFilterMojo extends AbstractMojo {

	/**
	 * Collection of artifact definitions to exclude. The {@link Exclude} element defines
	 * a {@code groupId} and {@code artifactId} mandatory properties and an optional
	 * {@code classifier} property.
	 * @since 1.1
	 */
	@Parameter
	private List<Exclude> excludes;

	/**
	 * Comma separated list of groupId names to exclude (exact match).
	 * @since 1.1
	 */
	@Parameter(property = "excludeGroupIds", defaultValue = "")
	private String excludeGroupIds;

	/**
	 * Comma separated list of artifact names to exclude (exact match).
	 * @since 1.1
	 */
	@Parameter(property = "excludeArtifactIds", defaultValue = "")
	private String excludeArtifactIds;

	protected void setExcludes(List<Exclude> excludes) {
		this.excludes = excludes;
	}

	protected void setExcludeGroupIds(String excludeGroupIds) {
		this.excludeGroupIds = excludeGroupIds;
	}

	protected void setExcludeArtifactIds(String excludeArtifactIds) {
		this.excludeArtifactIds = excludeArtifactIds;
	}

	@SuppressWarnings("unchecked")
	protected Set<Artifact> filterDependencies(Set<Artifact> dependencies,
			FilterArtifacts filters) throws MojoExecutionException {
		try {
			return filters.filter(dependencies);
		}
		catch (ArtifactFilterException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * Return artifact filters configured for this MOJO.
	 * @param additionalFilters optional additional filters to apply
	 * @return the filters
	 */
	protected final FilterArtifacts getFilters(ArtifactsFilter... additionalFilters) {
		FilterArtifacts filters = new FilterArtifacts();
		for (ArtifactsFilter additionalFilter : additionalFilters) {
			filters.addFilter(additionalFilter);
		}
		filters.addFilter(new ArtifactIdFilter("",
				cleanFilterConfig(this.excludeArtifactIds)));
		filters.addFilter(new MatchingGroupIdFilter(
				cleanFilterConfig(this.excludeGroupIds)));
		if (this.excludes != null) {
			filters.addFilter(new ExcludeFilter(this.excludes));
		}
		return filters;
	}

	private String cleanFilterConfig(String content) {
		if (content == null || content.trim().isEmpty()) {
			return "";
		}
		StringBuilder cleaned = new StringBuilder();
		StringTokenizer tokenizer = new StringTokenizer(content, ",");
		while (tokenizer.hasMoreElements()) {
			cleaned.append(tokenizer.nextToken().trim());
			if (tokenizer.hasMoreElements()) {
				cleaned.append(",");
			}
		}
		return cleaned.toString();
	}
}
