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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 
 * @author Stephane Nicoll
 */
public class DependencyFilterMojoTests {

	@Test
	public void filterDependencies() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude> emptyList(), "com.foo", "exclude-id");

		Artifact artifact = createArtifact("com.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				createArtifact("com.bar", "exclude-id"), artifact);
		assertEquals("wrong filtering of artifacts", 1, artifacts.size());
		assertSame("Wrong filtered artifact", artifact, artifacts.iterator().next());
	}

	@Test
	public void filterGroupIdExactMatch() throws MojoExecutionException {
		TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(
				Collections.<Exclude>emptyList(), "com.foo", "");

		Artifact artifact = createArtifact("com.foo.bar", "one");
		Set<Artifact> artifacts = mojo.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				artifact);
		assertEquals("wrong filtering of artifacts", 1, artifacts.size());
		assertSame("Wrong filtered artifact", artifact, artifacts.iterator().next());
	}

	private Artifact createArtifact(String groupId, String artifactId) {
		Artifact a = mock(Artifact.class);
		given(a.getGroupId()).willReturn(groupId);
		given(a.getArtifactId()).willReturn(artifactId);
		return a;
	}

	private static class TestableDependencyFilterMojo extends
			AbstractDependencyFilterMojo {

		private TestableDependencyFilterMojo(List<Exclude> excludes,
				String excludeGroupIds, String excludeArtifactIds) {
			setExcludes(excludes);
			setExcludeGroupIds(excludeGroupIds);
			setExcludeArtifactIds(excludeArtifactIds);
		}

		public Set<Artifact> filterDependencies(Artifact... artifacts)
				throws MojoExecutionException {
			Set<Artifact> input = new HashSet<Artifact>(Arrays.asList(artifacts));
			return filterDependencies(input, getFilters());
		}

		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {

		}
	}
}
