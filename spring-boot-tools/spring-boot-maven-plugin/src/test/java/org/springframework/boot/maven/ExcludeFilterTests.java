/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ExcludeFilter}.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ExcludeFilterTests {

	@Test
	public void excludeSimple() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(
				Arrays.asList(createExclude("com.foo", "bar")));
		Set result = filter
				.filter(Collections.singleton(createArtifact("com.foo", "bar")));
		assertThat(result).isEmpty();
	}

	@Test
	public void excludeGroupIdNoMatch() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(
				Arrays.asList(createExclude("com.foo", "bar")));
		Artifact artifact = createArtifact("com.baz", "bar");
		Set result = filter.filter(Collections.singleton(artifact));
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void excludeArtifactIdNoMatch() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(
				Arrays.asList(createExclude("com.foo", "bar")));
		Artifact artifact = createArtifact("com.foo", "biz");
		Set result = filter.filter(Collections.singleton(artifact));
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void excludeClassifier() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(
				Arrays.asList(createExclude("com.foo", "bar", "jdk5")));
		Set result = filter
				.filter(Collections.singleton(createArtifact("com.foo", "bar", "jdk5")));
		assertThat(result).isEmpty();
	}

	@Test
	public void excludeClassifierNoTargetClassifier() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(
				Arrays.asList(createExclude("com.foo", "bar", "jdk5")));
		Artifact artifact = createArtifact("com.foo", "bar");
		Set result = filter.filter(Collections.singleton(artifact));
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void excludeClassifierNoMatch() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(
				Arrays.asList(createExclude("com.foo", "bar", "jdk5")));
		Artifact artifact = createArtifact("com.foo", "bar", "jdk6");
		Set result = filter.filter(Collections.singleton(artifact));
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void excludeMulti() throws ArtifactFilterException {
		ExcludeFilter filter = new ExcludeFilter(Arrays.asList(
				createExclude("com.foo", "bar"), createExclude("com.foo", "bar2"),
				createExclude("org.acme", "app")));
		Set<Artifact> artifacts = new HashSet<Artifact>();
		artifacts.add(createArtifact("com.foo", "bar"));
		artifacts.add(createArtifact("com.foo", "bar"));
		Artifact anotherAcme = createArtifact("org.acme", "another-app");
		artifacts.add(anotherAcme);
		Set result = filter.filter(artifacts);
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isSameAs(anotherAcme);
	}

	private Exclude createExclude(String groupId, String artifactId) {
		return createExclude(groupId, artifactId, null);
	}

	private Exclude createExclude(String groupId, String artifactId, String classifier) {
		Exclude exclude = new Exclude();
		exclude.setGroupId(groupId);
		exclude.setArtifactId(artifactId);
		if (classifier != null) {
			exclude.setClassifier(classifier);
		}
		return exclude;
	}

	private Artifact createArtifact(String groupId, String artifactId,
			String classifier) {
		Artifact a = mock(Artifact.class);
		given(a.getGroupId()).willReturn(groupId);
		given(a.getArtifactId()).willReturn(artifactId);
		given(a.getClassifier()).willReturn(classifier);
		return a;
	}

	private Artifact createArtifact(String groupId, String artifactId) {
		return createArtifact(groupId, artifactId, null);
	}

}
