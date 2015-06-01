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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * {@link Dependencies} used by various spring boot tools. Provides programmatic access to
 * 'spring-boot-dependencies' and can also support user defined version managed
 * dependencies.
 *
 * @author Phillip Webb
 * @see Dependency
 */
public abstract class ManagedDependencies implements Dependencies {

	// NOTE: Take care if changing the API of this class, it is used by the third-party
	// Gretty tool (https://github.com/akhikhl/gretty)

	private final Dependencies delegate;

	ManagedDependencies(Dependencies delegate) {
		this.delegate = delegate;
	}

	/**
	 * Return the 'spring-boot-dependencies' POM version.
	 * @deprecated since 1.1.0 in favor of {@link #getSpringBootVersion()}
	 */
	@Deprecated
	public String getVersion() {
		return getSpringBootVersion();
	}

	/**
	 * Return the 'spring-boot-dependencies' POM version.
	 */
	public String getSpringBootVersion() {
		Dependency dependency = find("org.springframework.boot", "spring-boot");
		return (dependency == null ? null : dependency.getVersion());
	}

	/**
	 * Find a single dependency for the given group and artifact IDs.
	 * @param groupId the group ID
	 * @param artifactId the artifact ID
	 * @return a {@link Dependency} or {@code null}
	 */
	@Override
	public Dependency find(String groupId, String artifactId) {
		return this.delegate.find(groupId, artifactId);
	}

	/**
	 * Find a single dependency for the artifact IDs.
	 * @param artifactId the artifact ID
	 * @return a {@link Dependency} or {@code null}
	 */
	@Override
	public Dependency find(String artifactId) {
		return this.delegate.find(artifactId);
	}

	/**
	 * Provide an {@link Iterator} over all managed {@link Dependency Dependencies}.
	 */
	@Override
	public Iterator<Dependency> iterator() {
		return this.delegate.iterator();
	}

	/**
	 * Return spring-boot managed dependencies.
	 * @return The dependencies.
	 * @see #get(Collection)
	 */
	public static ManagedDependencies get() {
		return get(Collections.<Dependencies> emptySet());
	}

	/**
	 * Return spring-boot managed dependencies with optional version managed dependencies.
	 * @param versionManagedDependencies a collection of {@link Dependencies} that take
	 * precedence over the {@literal spring-boot-dependencies}.
	 * @return the dependencies
	 * @since 1.1.0
	 */
	public static ManagedDependencies get(
			Collection<Dependencies> versionManagedDependencies) {
		return new ManagedDependencies(new ManagedDependenciesDelegate(
				versionManagedDependencies)) {
		};
	}

}
