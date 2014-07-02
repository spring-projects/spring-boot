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

import java.io.InputStream;
import java.util.Collection;

/**
 * {@link Dependencies} delegate used internally by {@link ManagedDependencies}.
 *
 * @author Phillip Webb
 * @since 1.1.0
 */
class ManagedDependenciesDelegate extends AbstractDependencies {

	private static Dependencies springBootDependencies;

	/**
	 * Create a new {@link ManagedDependenciesDelegate} instance with optional version
	 * managed dependencies.
	 * @param versionManagedDependencies a collection of {@link Dependencies} that take
	 * precedence over the `spring-boot-dependencies`.
	 */
	public ManagedDependenciesDelegate(Collection<Dependencies> versionManagedDependencies) {
		this(getSpringBootDependencies(), versionManagedDependencies);
	}

	ManagedDependenciesDelegate(Dependencies rootDependencies,
			Collection<Dependencies> versionManagedDependencies) {
		addAll(rootDependencies);
		if (versionManagedDependencies != null) {
			for (Dependencies managedDependencies : versionManagedDependencies) {
				addAll(managedDependencies);
			}
		}
	}

	private void addAll(Dependencies dependencies) {
		for (Dependency dependency : dependencies) {
			add(new ArtifactAndGroupId(dependency), dependency);
		}
	}

	private static Dependencies getSpringBootDependencies() {
		if (springBootDependencies == null) {
			springBootDependencies = new PomDependencies(getResource("effective-pom.xml"));
		}
		return springBootDependencies;
	}

	private static InputStream getResource(String name) {
		InputStream inputStream = ManagedDependenciesDelegate.class
				.getResourceAsStream(name);
		Assert.notNull(inputStream, "Unable to load " + name);
		return inputStream;
	}

}
