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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.boot.dependency.tools.PomManagedDependencies;
import org.springframework.boot.dependency.tools.VersionManagedDependencies;

/**
 * Factory to create Maven {@link Dependency} objects from Boot
 * {@link PomManagedDependencies}.
 *
 * @author Phillip Webb
 */
public class ManagedDependenciesFactory {

	private final ManagedDependencies dependencies;

	ManagedDependenciesFactory() {
		this(new VersionManagedDependencies());
	}

	public ManagedDependenciesFactory(ManagedDependencies dependencies) {
		this.dependencies = dependencies;
	}

	/**
	 * Return a list of the managed dependencies.
	 */
	public List<Dependency> getManagedDependencies() {
		List<Dependency> result = new ArrayList<Dependency>();
		for (org.springframework.boot.dependency.tools.Dependency dependency : this.dependencies) {
			Artifact artifact = asArtifact(dependency);
			result.add(new Dependency(artifact, JavaScopes.COMPILE));
		}
		return result;
	}

	private Artifact asArtifact(
			org.springframework.boot.dependency.tools.Dependency dependency) {
		return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
				"jar", dependency.getVersion());
	}
}
