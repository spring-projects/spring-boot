/*
 * Copyright 2012-2021 the original author or authors.
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

package io.spring.concourse.releasescripts.sonatype;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.PathResource;

/**
 * Collects artifacts to be deployed.
 *
 * @author Andy Wilkinson
 */
class ArtifactCollector {

	private final Predicate<Path> excludeFilter;

	ArtifactCollector(List<String> exclude) {
		this.excludeFilter = excludeFilter(exclude);
	}

	private Predicate<Path> excludeFilter(List<String> exclude) {
		Predicate<String> patternFilter = exclude.stream().map(Pattern::compile).map(Pattern::asPredicate)
				.reduce((path) -> false, Predicate::or).negate();
		return (path) -> patternFilter.test(path.toString());
	}

	Collection<DeployableArtifact> collectArtifacts(Path root) {
		try (Stream<Path> artifacts = Files.walk(root)) {
			return artifacts.filter(Files::isRegularFile).filter(this.excludeFilter)
					.map((artifact) -> deployableArtifact(artifact, root)).toList();
		}
		catch (IOException ex) {
			throw new RuntimeException("Could not read artifacts from '" + root + "'");
		}
	}

	private DeployableArtifact deployableArtifact(Path artifact, Path root) {
		return new DeployableArtifact(new PathResource(artifact), root.relativize(artifact).toString());
	}

}
