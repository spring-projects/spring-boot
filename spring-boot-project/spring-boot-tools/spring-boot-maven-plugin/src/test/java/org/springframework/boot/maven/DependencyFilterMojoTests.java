/*
 * Copyright 2012-2020 the original author or authors.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractDependencyFilterMojo}.
 *
 * @author Stephane Nicoll
 */
class DependencyFilterMojoTests {

	@TempDir
	static Path temp;

	@Test
	void filterDependencies() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "com.foo");

		Artifact artifact = createArtifact("com.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(createArtifact("com.foo", "one"),
				createArtifact("com.foo", "two"), artifact);
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.iterator().next()).isSameAs(artifact);
	}

	@Test
	void filterGroupIdExactMatch() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "com.foo");

		Artifact artifact = createArtifact("com.foo.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(createArtifact("com.foo", "one"),
				createArtifact("com.foo", "two"), artifact);
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.iterator().next()).isSameAs(artifact);
	}

	@Test
	void filterScopeKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "",
				new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.foo", "two", Artifact.SCOPE_SYSTEM);
		Artifact three = createArtifact("com.foo", "three", Artifact.SCOPE_RUNTIME);
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three);
		assertThat(artifacts).containsExactly(one, three);
	}

	@Test
	void filterGroupIdKeepOrder() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "com.foo");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(two, three);
	}

	@Test
	void filterExcludeKeepOrder() throws MojoExecutionException {
		Exclude exclude = new Exclude();
		exclude.setGroupId("com.bar");
		exclude.setArtifactId("two");
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.singletonList(exclude), "");
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(one, three, four);
	}

	@Test
	void excludeByJarType() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "");
		Artifact one = createArtifact("com.foo", "one", null, "dependencies-starter");
		Artifact two = createArtifact("com.bar", "two");
		Set<Artifact> artifacts = mojo.filterDependencies(one, two);
		assertThat(artifacts).containsExactly(two);
	}

	private static Artifact createArtifact(String groupId, String artifactId) {
		return createArtifact(groupId, artifactId, null);
	}

	private static Artifact createArtifact(String groupId, String artifactId, String scope) {
		return createArtifact(groupId, artifactId, scope, null);
	}

	private static Artifact createArtifact(String groupId, String artifactId, String scope, String jarType) {
		Artifact a = mock(Artifact.class);
		given(a.getGroupId()).willReturn(groupId);
		given(a.getArtifactId()).willReturn(artifactId);
		if (scope != null) {
			given(a.getScope()).willReturn(scope);
		}
		given(a.getFile()).willReturn(createArtifactFile(jarType));
		return a;
	}

	private static File createArtifactFile(String jarType) {
		Path jarPath = temp.resolve(UUID.randomUUID().toString() + ".jar");
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		if (jarType != null) {
			manifest.getMainAttributes().putValue("Spring-Boot-Jar-Type", jarType);
		}
		try {
			new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest).close();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return jarPath.toFile();
	}

	private static final class TestableDependencyFilterMojo extends AbstractDependencyFilterMojo {

		private final ArtifactsFilter[] additionalFilters;

		private TestableDependencyFilterMojo(List<Exclude> excludes, String excludeGroupIds,
				ArtifactsFilter... additionalFilters) {
			setExcludes(excludes);
			setExcludeGroupIds(excludeGroupIds);
			this.additionalFilters = additionalFilters;
		}

		Set<Artifact> filterDependencies(Artifact... artifacts) throws MojoExecutionException {
			Set<Artifact> input = new LinkedHashSet<>(Arrays.asList(artifacts));
			return filterDependencies(input, getFilters(this.additionalFilters));
		}

		@Override
		public void execute() {

		}

	}

}
