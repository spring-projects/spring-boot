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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractDependencyFilterMojo}.
 *
 * @author Stephane Nicoll
 */
public class DependencyFilterMojoTests {

	@Test
	public void filterDependencies() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude>emptyList(), "com.foo", "exclude-id");

		Artifact artifact = createArtifact("com.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				createArtifact("com.bar", "exclude-id"), artifact);
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void filterGroupIdExactMatch() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude>emptyList(), "com.foo", "");

		Artifact artifact = createArtifact("com.foo.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				artifact);
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void filterScopeKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude>emptyList(), "", "",
				new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.foo", "two", Artifact.SCOPE_SYSTEM);
		Artifact three = createArtifact("com.foo", "three", Artifact.SCOPE_RUNTIME);
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three);
		assertThat(artifacts).containsExactly(one, three);
	}

	@Test
	public void filterArtifactIdKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude>emptyList(), "", "one,three");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.foo", "two");
		Artifact three = createArtifact("com.foo", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(two, four);
	}

	@Test
	public void filterGroupIdKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude>emptyList(), "com.foo", "");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(two, three);
	}

	@Test
	public void filterExcludeKeepOrder() throws MojoExecutionException {
		Exclude exclude = new Exclude();
		exclude.setGroupId("com.bar");
		exclude.setArtifactId("two");
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.singletonList(exclude), "", "");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(one, three, four);
	}

	private static Artifact createArtifact(String groupId, String artifactId) {
		return createArtifact(groupId, artifactId, null);
	}

	private static Artifact createArtifact(String groupId, String artifactId,
			String scope) {
		Artifact a = mock(Artifact.class);
		given(a.getGroupId()).willReturn(groupId);
		given(a.getArtifactId()).willReturn(artifactId);
		if (scope != null) {
			given(a.getScope()).willReturn(scope);
		}
		return a;
	}

	private static final class TestableDependencyFilterMojo
			extends AbstractDependencyFilterMojo {

		private final ArtifactsFilter[] additionalFilters;

		private TestableDependencyFilterMojo(List<Exclude> excludes,
				String excludeGroupIds, String excludeArtifactIds,
				ArtifactsFilter... additionalFilters) {
			setExcludes(excludes);
			setExcludeGroupIds(excludeGroupIds);
			setExcludeArtifactIds(excludeArtifactIds);
			this.additionalFilters = additionalFilters;
		}

		public Set<Artifact> filterDependencies(Artifact... artifacts)
				throws MojoExecutionException {
			Set<Artifact> input = new LinkedHashSet<Artifact>(Arrays.asList(artifacts));
			return filterDependencies(input, getFilters(this.additionalFilters));
		}

		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {

		}

	}

}
