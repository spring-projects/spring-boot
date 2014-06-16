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

package org.springframework.boot.cli.command.jar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.springframework.boot.cli.command.jar.ResourceMatcher.MatchedResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ResourceMatcher}.
 * 
 * @author Andy Wilkinson
 */
public class ResourceMatcherTests {

	@Test
	public void nonExistentRoot() throws IOException {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("alpha/**",
				"bravo/*", "*"), Arrays.asList(".*", "alpha/**/excluded"));
		List<MatchedResource> matchedResources = resourceMatcher.find(Arrays
				.asList(new File("does-not-exist")));
		assertEquals(0, matchedResources.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void defaults() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList(""),
				Arrays.asList(""));
		assertTrue(((Collection<String>) ReflectionTestUtils.getField(resourceMatcher,
				"includes")).contains("static/**"));
		assertTrue(((Collection<String>) ReflectionTestUtils.getField(resourceMatcher,
				"excludes")).contains("**/*.jar"));
	}

	@Test
	public void excludedWins() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("*"),
				Arrays.asList("**/*.jar"));
		List<MatchedResource> found = resourceMatcher.find(Arrays.asList(new File(
				"src/test/resources")));
		assertThat(found, not(hasItem(new FooJarMatcher(MatchedResource.class))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void includedDeltas() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(
				Arrays.asList("-static/**"), Arrays.asList(""));
		Collection<String> includes = (Collection<String>) ReflectionTestUtils.getField(
				resourceMatcher, "includes");
		assertTrue(includes.contains("templates/**"));
		assertFalse(includes.contains("static/**"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void includedDeltasAndNewEntries() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("-static/**",
				"foo.jar"), Arrays.asList("-**/*.jar"));
		Collection<String> includes = (Collection<String>) ReflectionTestUtils.getField(
				resourceMatcher, "includes");
		assertTrue(includes.contains("foo.jar"));
		assertTrue(includes.contains("templates/**"));
		assertFalse(includes.contains("static/**"));
		assertFalse(((Collection<String>) ReflectionTestUtils.getField(resourceMatcher,
				"excludes")).contains("**/*.jar"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void excludedDeltas() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList(""),
				Arrays.asList("-**/*.jar"));
		assertFalse(((Collection<String>) ReflectionTestUtils.getField(resourceMatcher,
				"excludes")).contains("**/*.jar"));
	}

	@Test
	public void jarFileAlwaysMatches() throws Exception {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("*"),
				Arrays.asList("**/*.jar"));
		List<MatchedResource> found = resourceMatcher.find(Arrays.asList(new File(
				"src/test/resources/templates"), new File("src/test/resources/foo.jar")));
		FooJarMatcher matcher = new FooJarMatcher(MatchedResource.class);
		assertThat(found, hasItem(matcher));
		// A jar file is always treated as a dependency (stick it in /lib)
		assertTrue(matcher.getMatched().isRoot());
	}

	@Test
	public void resourceMatching() throws IOException {
		ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList("alpha/**",
				"bravo/*", "*"), Arrays.asList(".*", "alpha/**/excluded"));
		List<MatchedResource> matchedResources = resourceMatcher.find(Arrays.asList(
				new File("src/test/resources/resource-matcher/one"), new File(
						"src/test/resources/resource-matcher/two"), new File(
						"src/test/resources/resource-matcher/three")));
		System.out.println(matchedResources);
		List<String> paths = new ArrayList<String>();
		for (MatchedResource resource : matchedResources) {
			paths.add(resource.getName());
		}

		assertEquals(6, paths.size());
		assertTrue(paths.containsAll(Arrays.asList("alpha/nested/fileA", "bravo/fileC",
				"fileD", "bravo/fileE", "fileF", "three")));
	}

	private final class FooJarMatcher extends TypeSafeMatcher<MatchedResource> {

		private MatchedResource matched;

		public MatchedResource getMatched() {
			return this.matched;
		}

		private FooJarMatcher(Class<?> expectedType) {
			super(expectedType);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("foo.jar");
		}

		@Override
		protected boolean matchesSafely(MatchedResource item) {
			boolean matches = item.getFile().getName().equals("foo.jar");
			if (matches) {
				this.matched = item;
			}
			return matches;
		}
	}

}
