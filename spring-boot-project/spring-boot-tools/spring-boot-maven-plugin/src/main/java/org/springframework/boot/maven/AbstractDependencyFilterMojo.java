/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
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

	static final ExcludeFilter DEVTOOLS_EXCLUDE_FILTER;
	static {
		Exclude exclude = new Exclude();
		exclude.setGroupId("org.springframework.boot");
		exclude.setArtifactId("spring-boot-devtools");
		DEVTOOLS_EXCLUDE_FILTER = new ExcludeFilter(exclude);
	}

	static final ExcludeFilter DOCKER_COMPOSE_EXCLUDE_FILTER;
	static {
		Exclude exclude = new Exclude();
		exclude.setGroupId("org.springframework.boot");
		exclude.setArtifactId("spring-boot-docker-compose");
		DOCKER_COMPOSE_EXCLUDE_FILTER = new ExcludeFilter(exclude);
	}

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

	/**
     * Sets the list of excludes for the dependency filter.
     * 
     * @param excludes the list of excludes to set
     */
    protected void setExcludes(List<Exclude> excludes) {
		this.excludes = excludes;
	}

	/**
     * Sets the list of includes for the dependency filter.
     * 
     * @param includes the list of includes to be set
     */
    protected void setIncludes(List<Include> includes) {
		this.includes = includes;
	}

	/**
     * Sets the excludeGroupIds property.
     * 
     * @param excludeGroupIds the comma-separated list of groupIds to exclude
     */
    protected void setExcludeGroupIds(String excludeGroupIds) {
		this.excludeGroupIds = excludeGroupIds;
	}

	/**
     * Retrieves the URLs of the dependencies based on the provided filters.
     * 
     * @param additionalFilters The additional filters to be applied on top of the default filters.
     * @return The list of URLs representing the dependencies.
     * @throws MojoExecutionException If an error occurs during the execution of the Mojo.
     */
    protected List<URL> getDependencyURLs(ArtifactsFilter... additionalFilters) throws MojoExecutionException {
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), additionalFilters);
		List<URL> urls = new ArrayList<>();
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				urls.add(toURL(artifact.getFile()));
			}
		}
		return urls;
	}

	/**
     * Filters the given set of dependencies based on the provided additional filters.
     * 
     * @param dependencies the set of dependencies to be filtered
     * @param additionalFilters the additional filters to be applied
     * @return the filtered set of dependencies
     * @throws MojoExecutionException if an error occurs during the filtering process
     */
    protected final Set<Artifact> filterDependencies(Set<Artifact> dependencies, ArtifactsFilter... additionalFilters)
			throws MojoExecutionException {
		try {
			Set<Artifact> filtered = new LinkedHashSet<>(dependencies);
			filtered.retainAll(getFilters(additionalFilters).filter(dependencies));
			return filtered;
		}
		catch (ArtifactFilterException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	/**
     * Converts a File object to a URL object.
     *
     * @param file the File object to be converted
     * @return the URL object representing the file's location
     * @throws IllegalStateException if the URL for the file is invalid
     */
    protected URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid URL for " + file, ex);
		}
	}

	/**
	 * Return artifact filters configured for this MOJO.
	 * @param additionalFilters optional additional filters to apply
	 * @return the filters
	 */
	private FilterArtifacts getFilters(ArtifactsFilter... additionalFilters) {
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

	/**
     * Cleans the filter configuration by removing any leading or trailing whitespace and empty elements.
     * 
     * @param content the filter configuration content to be cleaned
     * @return the cleaned filter configuration content
     */
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

	/**
	 * {@link ArtifactFilter} to exclude test scope dependencies.
	 */
	protected static class ExcludeTestScopeArtifactFilter extends AbstractArtifactFeatureFilter {

		/**
         * Creates a new instance of ExcludeTestScopeArtifactFilter.
         * This filter excludes artifacts with the test scope.
         */
        ExcludeTestScopeArtifactFilter() {
			super("", Artifact.SCOPE_TEST);
		}

		/**
         * Returns the scope of the given artifact.
         *
         * @param artifact the artifact to get the scope from
         * @return the scope of the artifact
         */
        @Override
		protected String getArtifactFeature(Artifact artifact) {
			return artifact.getScope();
		}

	}

	/**
	 * {@link ArtifactFilter} that only include runtime scopes.
	 */
	protected static class RuntimeArtifactFilter implements ArtifactFilter {

		private static final Collection<String> SCOPES = List.of(Artifact.SCOPE_COMPILE,
				Artifact.SCOPE_COMPILE_PLUS_RUNTIME, Artifact.SCOPE_RUNTIME);

		/**
         * Determines whether or not to include the given artifact.
         * 
         * @param artifact the artifact to be checked
         * @return true if the artifact should be included, false otherwise
         */
        @Override
		public boolean include(Artifact artifact) {
			String scope = artifact.getScope();
			return !artifact.isOptional() && (scope == null || SCOPES.contains(scope));
		}

	}

}
