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
import java.util.List;

import org.junit.Test;
import org.springframework.boot.cli.command.jar.ResourceMatcher.MatchedResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ResourceMatcher}.
 * 
 * @author Andy Wilkinson
 */
public class ResourceMatcherTests {

	private final ResourceMatcher resourceMatcher = new ResourceMatcher(Arrays.asList(
			"alpha/**", "bravo/*", "*"), Arrays.asList(".*", "alpha/**/excluded"));

	@Test
	public void nonExistentRoot() throws IOException {
		List<MatchedResource> matchedResources = this.resourceMatcher.find(Arrays
				.asList(new File("does-not-exist")));
		assertEquals(0, matchedResources.size());
	}

	@Test
	public void resourceMatching() throws IOException {
		List<MatchedResource> matchedResources = this.resourceMatcher.find(Arrays.asList(
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
}
