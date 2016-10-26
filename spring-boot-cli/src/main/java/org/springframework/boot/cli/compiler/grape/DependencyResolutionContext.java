/*
 * Copyright 2012-2015 the original author or authors.
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

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.util.artifact.JavaScopes;

import org.springframework.boot.aether.DependencyManagementContext;
import org.springframework.boot.cli.compiler.dependencies.ArtifactCoordinatesResolver;
import org.springframework.boot.cli.compiler.dependencies.CompositeDependencyManagement;
import org.springframework.boot.cli.compiler.dependencies.DependencyManagement;
import org.springframework.boot.cli.compiler.dependencies.DependencyManagementArtifactCoordinatesResolver;
import org.springframework.boot.cli.compiler.dependencies.SpringBootDependenciesDependencyManagement;

/**
 * Context used when resolving dependencies.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class DependencyResolutionContext extends DependencyManagementContext {

	private DependencyManagement dependencyManagement = null;

	private ArtifactCoordinatesResolver artifactCoordinatesResolver;

	public DependencyResolutionContext() {
		addDependencyManagement(new SpringBootDependenciesDependencyManagement());
	}

	public ArtifactCoordinatesResolver getArtifactCoordinatesResolver() {
		return this.artifactCoordinatesResolver;
	}

	public void addDependencyManagement(DependencyManagement dependencyManagement) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (org.springframework.boot.cli.compiler.dependencies.Dependency dependency : dependencyManagement
				.getDependencies()) {
			List<Exclusion> aetherExclusions = new ArrayList<Exclusion>();
			for (org.springframework.boot.cli.compiler.dependencies.Dependency.Exclusion exclusion : dependency
					.getExclusions()) {
				aetherExclusions.add(new Exclusion(exclusion.getGroupId(),
						exclusion.getArtifactId(), "*", "*"));
			}
			Dependency aetherDependency = new Dependency(
					new DefaultArtifact(dependency.getGroupId(),
							dependency.getArtifactId(), "jar", dependency.getVersion()),
					JavaScopes.COMPILE, false, aetherExclusions);
			dependencies.add(aetherDependency);
		}
		addManagedDependencies(dependencies);
		this.dependencyManagement = this.dependencyManagement == null
				? dependencyManagement
				: new CompositeDependencyManagement(dependencyManagement,
						this.dependencyManagement);
		this.artifactCoordinatesResolver = new DependencyManagementArtifactCoordinatesResolver(
				this.dependencyManagement);
	}

}
