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
import java.util.Collections;

/**
 * {@link ManagedDependencies} used by various spring boot tools. Provides programmatic
 * access to 'spring-boot-dependencies' and can also support user defined version managed
 * dependencies.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 */
public class VersionManagedDependencies extends AbstractManagedDependencies {

	private static ManagedDependencies springBootDependencies;

	/**
	 * Create a new {@link VersionManagedDependencies} instance for
	 * 'spring-boot-dependencies'.
	 */
	public VersionManagedDependencies() {
		this(Collections.<ManagedDependencies> emptySet());
	}

	/**
	 * Create a new {@link VersionManagedDependencies} instance with optional version
	 * managed dependencies.
	 * @param versionManagedDependencies a collection of {@link ManagedDependencies} that
	 * take precedence over the `spring-boot-dependencies`.
	 */
	public VersionManagedDependencies(
			Collection<ManagedDependencies> versionManagedDependencies) {
		this(getSpringBootDependencies(), versionManagedDependencies);
	}

	VersionManagedDependencies(ManagedDependencies rootDependencies,
			Collection<ManagedDependencies> versionManagedDependencies) {
		addAll(rootDependencies);
		if (versionManagedDependencies != null) {
			for (ManagedDependencies managedDependencies : versionManagedDependencies) {
				addAll(managedDependencies);
			}
		}
	}

	private void addAll(ManagedDependencies dependencies) {
		for (Dependency dependency : dependencies) {
			add(new ArtifactAndGroupId(dependency), dependency);
		}
	}

	private static ManagedDependencies getSpringBootDependencies() {
		if (springBootDependencies == null) {
			springBootDependencies = new PomManagedDependencies(
					getResource("effective-pom.xml"), getResource("dependencies-pom.xml"));
		}
		return springBootDependencies;
	}

	private static InputStream getResource(String name) {
		InputStream inputStream = VersionManagedDependencies.class
				.getResourceAsStream(name);
		Assert.notNull(inputStream, "Unable to load " + name);
		return inputStream;
	}

}
