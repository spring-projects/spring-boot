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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.dependency.tools.Dependency.Exclusion;
import org.springframework.boot.dependency.tools.Dependency.ExclusionType;

/**
 * {@link Dependencies} to extend an existing {@link Dependencies} instance with
 * transitive {@link Exclusion}s located from a {@link DependencyTree}.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 */
class DependenciesWithTransitiveExclusions extends AbstractDependencies {

	public DependenciesWithTransitiveExclusions(Dependencies dependencies,
			DependencyTree tree) {
		DependencyBuilder builder = new DependencyBuilder(dependencies);
		builder.addTransitiveExcludes(tree);
		builder.finish();
	}

	/**
	 * Builder used to collect the transitive exclusions.
	 */
	private class DependencyBuilder {

		private Map<ArtifactAndGroupId, DependencyAndTransitiveExclusions> dependencies;

		public DependencyBuilder(Dependencies dependencies) {
			this.dependencies = new LinkedHashMap<ArtifactAndGroupId, DependencyAndTransitiveExclusions>();
			for (Dependency dependency : dependencies) {
				this.dependencies.put(new ArtifactAndGroupId(dependency),
						new DependencyAndTransitiveExclusions(dependency));
			}
		}

		public void addTransitiveExcludes(DependencyTree tree) {
			for (DependencyNode node : tree) {
				DependencyAndTransitiveExclusions dependency = this.dependencies
						.get(asArtifactAndGroupId(node));
				if (dependency != null) {
					for (DependencyNode child : node) {
						addTransitiveExcludes(dependency, child);
					}
				}
			}
		}

		private void addTransitiveExcludes(DependencyAndTransitiveExclusions dependency,
				DependencyNode node) {
			DependencyAndTransitiveExclusions exclusions = this.dependencies
					.get(asArtifactAndGroupId(node));
			if (exclusions != null) {
				dependency.addTransitiveExclusions(exclusions.getSourceDependency());
			}
			for (DependencyNode child : node) {
				addTransitiveExcludes(dependency, child);
			}
		}

		private ArtifactAndGroupId asArtifactAndGroupId(DependencyNode node) {
			return new ArtifactAndGroupId(node.getGroupId(), node.getArtifactId());
		}

		public void finish() {
			for (Map.Entry<ArtifactAndGroupId, DependencyAndTransitiveExclusions> entry : this.dependencies
					.entrySet()) {
				add(entry.getKey(), entry.getValue().createNewDependency());
			}
		}

	}

	/**
	 * Holds a {@link Dependency} with additional transitive {@link Exclusion}s.
	 */
	private static class DependencyAndTransitiveExclusions {

		private Dependency dependency;

		private Set<Exclusion> transitiveExclusions = new LinkedHashSet<Exclusion>();

		public DependencyAndTransitiveExclusions(Dependency dependency) {
			this.dependency = dependency;
		}

		public Dependency getSourceDependency() {
			return this.dependency;
		}

		public void addTransitiveExclusions(Dependency dependency) {
			for (Exclusion exclusion : dependency.getExclusions()) {
				this.transitiveExclusions.add(new Exclusion(exclusion.getGroupId(),
						exclusion.getArtifactId(), ExclusionType.TRANSITIVE));
			}
		}

		public Dependency createNewDependency() {
			Set<Exclusion> exclusions = new LinkedHashSet<Dependency.Exclusion>();
			exclusions.addAll(this.dependency.getExclusions());
			exclusions.addAll(this.transitiveExclusions);
			return new Dependency(this.dependency.getGroupId(),
					this.dependency.getArtifactId(), this.dependency.getVersion(),
					new ArrayList<Exclusion>(exclusions));
		}

	}

}
