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

import java.net.URI;

import org.springframework.util.ObjectUtils;

/**
 * The configuration of a repository.
 *
 * @author Andy Wilkinson
 */
public final class RepositoryConfiguration {

	private final String name;

	private final URI uri;

	private final boolean snapshotsEnabled;

	/**
	 * Creates a new {@code RepositoryConfiguration} instance.
	 * @param name the name of the repository
	 * @param uri the uri of the repository
	 * @param snapshotsEnabled {@code true} if the repository should enable access to
	 * snapshots, {@code false} otherwise
	 */
	public RepositoryConfiguration(String name, URI uri, boolean snapshotsEnabled) {
		this.name = name;
		this.uri = uri;
		this.snapshotsEnabled = snapshotsEnabled;
	}

	/**
	 * Return the name of the repository.
	 * @return the repository name
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "RepositoryConfiguration [name=" + this.name + ", uri=" + this.uri
				+ ", snapshotsEnabled=" + this.snapshotsEnabled + "]";
	}

	/**
	 * Return the URI of the repository.
	 * @return the repository URI
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return if the repository should enable access to snapshots.
	 * @return {@code true} if snapshot access is enabled
	 */
	public boolean getSnapshotsEnabled() {
		return this.snapshotsEnabled;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RepositoryConfiguration other = (RepositoryConfiguration) obj;
		return ObjectUtils.nullSafeEquals(this.name, other.name);
	}

}
