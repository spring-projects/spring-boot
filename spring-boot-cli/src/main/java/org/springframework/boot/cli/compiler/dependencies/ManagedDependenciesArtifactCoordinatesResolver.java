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

package org.springframework.boot.cli.compiler.dependencies;

import org.springframework.boot.dependency.tools.Dependency;
import org.springframework.boot.dependency.tools.ManagedDependencies;

/**
 * {@link ArtifactCoordinatesResolver} backed by {@link ManagedDependencies}.
 * 
 * @author Phillip Webb
 */
public class ManagedDependenciesArtifactCoordinatesResolver implements
		ArtifactCoordinatesResolver {

	private final ManagedDependencies dependencies;

	public ManagedDependenciesArtifactCoordinatesResolver() {
		this(ManagedDependencies.get());
	}

	ManagedDependenciesArtifactCoordinatesResolver(ManagedDependencies dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public String getGroupId(String artifactId) {
		Dependency dependency = find(artifactId);
		return (dependency == null ? null : dependency.getGroupId());
	}

	@Override
	public String getVersion(String artifactId) {
		Dependency dependency = find(artifactId);
		return (dependency == null ? null : dependency.getVersion());
	}

	private Dependency find(String artifactId) {
		if (artifactId != null) {
			if (artifactId.startsWith("spring-boot")) {
				return new Dependency("org.springframework.boot", artifactId,
						this.dependencies.getVersion());
			}
			return this.dependencies.find(artifactId);
		}
		return null;
	}
}
