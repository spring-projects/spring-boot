/*
 * Copyright 2012-present the original author or authors.
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

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyFilter}.
 *
 * @author Phillip Webb
 */
class DependencyFilterTests {

	@Test
	void excludeFiltersBasedOnPredicate() throws ArtifactFilterException {
		DependencyFilter filter = DependencyFilter.exclude(Artifact::isOptional);
		ArtifactHandler ah = new DefaultArtifactHandler();
		VersionRange v = VersionRange.createFromVersion("1.0.0");
		DefaultArtifact a1 = new DefaultArtifact("com.example", "a1", v, "compile", "jar", null, ah, false);
		DefaultArtifact a2 = new DefaultArtifact("com.example", "a2", v, "compile", "jar", null, ah, true);
		DefaultArtifact a3 = new DefaultArtifact("com.example", "a3", v, "compile", "jar", null, ah, false);
		Set<Artifact> filtered = filter.filter(Set.of(a1, a2, a3));
		assertThat(filtered).containsExactlyInAnyOrder(a1, a3);
	}

}
