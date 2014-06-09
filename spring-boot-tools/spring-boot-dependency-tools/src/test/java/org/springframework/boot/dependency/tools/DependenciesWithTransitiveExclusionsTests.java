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

package org.springframework.boot.dependency.tools;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DependenciesWithTransitiveExclusions}.
 * 
 * @author Phillip Webb
 */
public class DependenciesWithTransitiveExclusionsTests {

	@Test
	public void findsTransitiveExclusions() throws Exception {
		Dependencies source = new PomDependencies(getClass().getResourceAsStream(
				"test-effective-pom.xml"));
		DependencyTree tree = new DependencyTree(getClass().getResourceAsStream(
				"test-effective-pom-dependency-tree.txt"));
		DependenciesWithTransitiveExclusions dependencies = new DependenciesWithTransitiveExclusions(
				source, tree);
		assertExcludes(dependencies, "sample01", "[org.exclude:exclude01]");
		assertExcludes(source, "sample02", "[]");
		assertExcludes(dependencies, "sample02", "[org.exclude:exclude01]");
	}

	private void assertExcludes(Dependencies dependencies, String artifactId,
			String expected) {
		Dependency dependency = dependencies.find("org.sample", artifactId);
		assertThat(dependency.getExclusions().toString(), equalTo(expected));
	}

}
