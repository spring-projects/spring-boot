/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractDependencyFilterMojo}.
 *
 * @author Dmytro Nosan
 */
public class DependencyIncludeFilterMojoTests {

	@Test
	public void filterDependencies() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.emptyList(), "com.foo");

		Artifact artifact = createArtifact("com.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				createArtifact("com.bar", "include-id"), artifact);
		assertThat(artifacts).hasSize(2);
	}

	@Test
	public void filterGroupIdExactMatch() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.emptyList(), "com.foo");

		Artifact artifact1 = createArtifact("com.foo", "one");
		Artifact artifact2 = createArtifact("com.foo", "two");
		Set<Artifact> artifacts = mojo.filterDependencies(
				artifact1, artifact2,
				createArtifact("com.foo.bar", "one"));
		assertThat(artifacts).hasSize(2);
		assertThat(artifacts)
				.contains(artifact2)
				.contains(artifact2);
	}

	@Test
	public void filterScopeKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.emptyList(), "",
				new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		Artifact artifact = createArtifact("com.foo", "two", Artifact.SCOPE_SYSTEM);
		Artifact artifact1 = createArtifact("com.foo", "three", Artifact.SCOPE_RUNTIME);
		Artifact artifact2 = createArtifact("com.foo", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(artifact, artifact1, artifact2);
		assertThat(artifacts).containsExactly(artifact1, artifact2);
	}



	@Test
	public void filterGroupIdKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.emptyList(), "com.foo");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(one, four);
	}

	@Test
	public void filterIncludeKeepOrder() throws MojoExecutionException {
		Include include1 = new Include();
		include1.setGroupId("com.bar");
		include1.setArtifactId("two");

		Include include2 = new Include();
		include2.setGroupId("com.bar");
		include2.setArtifactId("three");

		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Arrays.asList(include1, include2), "");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(two, three);
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

		private TestableDependencyFilterMojo(List<Include> includes, String includeGroupIds,
				ArtifactsFilter... additionalFilters) {
			setIncludes(includes);
			setIncludeGroupIds(includeGroupIds);
			this.additionalFilters = additionalFilters;
		}

		public Set<Artifact> filterDependencies(Artifact... artifacts)
				throws MojoExecutionException {
			Set<Artifact> input = new LinkedHashSet<>(Arrays.asList(artifacts));
			return filterDependencies(input, getFilters(this.additionalFilters));
		}

		@Override
		public void execute() {
		}

	}

}
