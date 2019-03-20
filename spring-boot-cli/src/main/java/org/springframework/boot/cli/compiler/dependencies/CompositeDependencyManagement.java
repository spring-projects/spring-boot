/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.cli.compiler.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link DependencyManagement} that delegates to one or more {@link DependencyManagement}
 * instances.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class CompositeDependencyManagement implements DependencyManagement {

	private final List<DependencyManagement> delegates;

	private final List<Dependency> dependencies = new ArrayList<Dependency>();

	public CompositeDependencyManagement(DependencyManagement... delegates) {
		this.delegates = Arrays.asList(delegates);
		for (DependencyManagement delegate : delegates) {
			this.dependencies.addAll(delegate.getDependencies());
		}
	}

	@Override
	public List<Dependency> getDependencies() {
		return this.dependencies;
	}

	@Override
	public String getSpringBootVersion() {
		for (DependencyManagement delegate : this.delegates) {
			String version = delegate.getSpringBootVersion();
			if (version != null) {
				return version;
			}
		}
		return null;
	}

	@Override
	public Dependency find(String artifactId) {
		for (DependencyManagement delegate : this.delegates) {
			Dependency found = delegate.find(artifactId);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

}
