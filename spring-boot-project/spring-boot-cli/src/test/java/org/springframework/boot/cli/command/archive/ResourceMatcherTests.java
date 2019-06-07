/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cli.command.archive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.Test;

import org.springframework.boot.cli.command.archive.ResourceMatcher.MatchedResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceMatcher}.
 *
 * @author Andy Wilkinson
 */
public class ResourceMatcherTests {

	@Test
	public void nonExistentRoot() throws IOException {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("alpha/**", "bravo/*", "*"),
				Arrays.asList(".*", "alpha/**/excluded"));
		List<MatchedResource> matchedResources = resourceMatcher.find(Arrays.asList(new File("does-not-exist")));
		assertThat(matchedResources).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void defaults() {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList(""), Arrays.asList(""));
		Collection<String> includes = (Collection<String>) ReflectionTestUtils.getField(resourceMatcher, "includes");
		Collection<String> excludes = (Collection<String>) ReflectionTestUtils.getField(resourceMatcher, "excludes");
		assertThat(includes).contains("static/**");
		assertThat(excludes).contains("**/*.jar");
	}

	@Test
	public void excludedWins() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("*"), Arrays.asList("**/*.jar"));
		List<MatchedResource> found = resourceMatcher.find(Arrays.asList(new File("src/test/resources")));
		assertThat(found).areNot(new Condition<MatchedResource>() {

			@Override
			public boolean matches(MatchedResource value) {
				return value.getFile().getName().equals("foo.jar");
			}

		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void includedDeltas() {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("-static/**"), Arrays.asList(""));
		Collection<String> includes = (Collection<String>) ReflectionTestUtils.getField(resourceMatcher, "includes");
		assertThat(includes).contains("templates/**");
		assertThat(includes).doesNotContain("static/**");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void includedDeltasAndNewEntries() {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("-static/**", "foo.jar"),
				Arrays.asList("-**/*.jar"));
		Collection<String> includes = (Collection<String>) ReflectionTestUtils.getField(resourceMatcher, "includes");
		Collection<String> excludes = (Collection<String>) ReflectionTestUtils.getField(resourceMatcher, "excludes");
		assertThat(includes).contains("foo.jar");
		assertThat(includes).contains("templates/**");
		assertThat(includes).doesNotContain("static/**");
		assertThat(excludes).doesNotContain("**/*.jar");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void excludedDeltas() {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList(""), Arrays.asList("-**/*.jar"));
		Collection<String> excludes = (Collection<String>) ReflectionTestUtils.getField(resourceMatcher, "excludes");
		assertThat(excludes).doesNotContain("**/*.jar");
	}

	@Test
	public void jarFileAlwaysMatches() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("*"), Arrays.asList("**/*.jar"));
		List<MatchedResource> found = resourceMatcher
				.find(Arrays.asList(new File("src/test/resources/templates"), new File("src/test/resources/foo.jar")));
		assertThat(found).areAtLeastOne(new Condition<MatchedResource>() {

			@Override
			public boolean matches(MatchedResource value) {
				return value.getFile().getName().equals("foo.jar") && value.isRoot();
			}

		});
	}

	@Test
	public void resourceMatching() throws IOException {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("alpha/**", "bravo/*", "*"),
				Arrays.asList(".*", "alpha/**/excluded"));
		List<MatchedResource> matchedResources = resourceMatcher
				.find(Arrays.asList(new File("src/test/resources/resource-matcher/one"),
						new File("src/test/resources/resource-matcher/two"),
						new File("src/test/resources/resource-matcher/three")));
		List<String> paths = new ArrayList<>();
		for (MatchedResource resource : matchedResources) {
			paths.add(resource.getName());
		}
		assertThat(paths).containsOnly("alpha/nested/fileA", "bravo/fileC", "fileD", "bravo/fileE", "fileF", "three");
	}

}
