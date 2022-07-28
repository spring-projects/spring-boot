/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

/**
 * A base mojo filtering the dependencies of the project.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.1.0
 */
public abstract class AbstractDependencyFilterMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 * @since 3.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	/**
	 * Collection of artifact definitions to include. The {@link Include} element defines
	 * mandatory {@code groupId} and {@code artifactId} properties and an optional
	 * mandatory {@code groupId} and {@code artifactId} properties and an optional
	 * {@code classifier} property.
	 * @since 1.2.0
	 */
	@Parameter(property = "spring-boot.includes")
	private List<Include> includes;

	/**
	 * Collection of artifact definitions to exclude. The {@link Exclude} element defines
	 * mandatory {@code groupId} and {@code artifactId} properties and an optional
	 * {@code classifier} property.
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.excludes")
	private List<Exclude> excludes;

	/**
	 * Comma separated list of groupId names to exclude (exact match).
	 * @since 1.1.0
	 */
	@Parameter(property = "spring-boot.excludeGroupIds", defaultValue = "")
	private String excludeGroupIds;

	protected void setExcludes(List<Exclude> excludes) {
		this.excludes = excludes;
	}

	protected void setIncludes(List<Include> includes) {
		this.includes = includes;
	}

	protected void setExcludeGroupIds(String excludeGroupIds) {
		this.excludeGroupIds = excludeGroupIds;
	}

	protected List<URL> getDependencyURLs(ArtifactsFilter... additionalFilters) throws MojoExecutionException {
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), getFilters(additionalFilters));
		List<URL> urls = new ArrayList<>();
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				urls.add(toURL(artifact.getFile()));
			}
		}
		return urls;
	}

	protected URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid URL for " + file, ex);
		}
	}

	protected final Set<Artifact> filterDependencies(Set<Artifact> dependencies, FilterArtifacts filters)
			throws MojoExecutionException {
		try {
			Set<Artifact> filtered = new LinkedHashSet<>(dependencies);
			filtered.retainAll(filters.filter(dependencies));
			return filtered;
		}
		catch (ArtifactFilterException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
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
		filters.addFilter(new MatchingGroupIdFilter(cleanFilterConfig(this.excludeGroupIds)));
		if (this.includes != null && !this.includes.isEmpty()) {
			filters.addFilter(new IncludeFilter(this.includes));
		}
		if (this.excludes != null && !this.excludes.isEmpty()) {
			filters.addFilter(new ExcludeFilter(this.excludes));
		}
		filters.addFilter(new JarTypeFilter());
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

	static class TestArtifactFilter extends AbstractArtifactFeatureFilter {

		TestArtifactFilter() {
			super("", Artifact.SCOPE_TEST);
		}

		@Override
		protected String getArtifactFeature(Artifact artifact) {
			return artifact.getScope();
		}

	}

}
