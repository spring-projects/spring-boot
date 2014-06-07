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

package org.springframework.boot.cli.compiler.grape;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.graph.Dependency;
import org.springframework.boot.cli.compiler.dependencies.ArtifactCoordinatesResolver;
import org.springframework.boot.cli.compiler.dependencies.ManagedDependenciesArtifactCoordinatesResolver;
import org.springframework.boot.dependency.tools.ManagedDependencies;

/**
 * Context used when resolving dependencies.
 * 
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class DependencyResolutionContext {

	private ArtifactCoordinatesResolver artifactCoordinatesResolver;

	private List<Dependency> managedDependencies = new ArrayList<Dependency>();

	public DependencyResolutionContext() {
		this(new ManagedDependenciesArtifactCoordinatesResolver());
	}

	public DependencyResolutionContext(
			ArtifactCoordinatesResolver artifactCoordinatesResolver) {
		this.artifactCoordinatesResolver = artifactCoordinatesResolver;
		this.managedDependencies = new ManagedDependenciesFactory()
				.getManagedDependencies();
	}

	public void setManagedDependencies(ManagedDependencies managedDependencies) {
		this.artifactCoordinatesResolver = new ManagedDependenciesArtifactCoordinatesResolver(
				managedDependencies);
		this.managedDependencies = new ArrayList<Dependency>(
				new ManagedDependenciesFactory(managedDependencies)
						.getManagedDependencies());
	}

	public ArtifactCoordinatesResolver getArtifactCoordinatesResolver() {
		return this.artifactCoordinatesResolver;
	}

	public List<Dependency> getManagedDependencies() {
		return this.managedDependencies;
	}
}
