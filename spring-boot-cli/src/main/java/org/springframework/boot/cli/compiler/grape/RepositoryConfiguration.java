/*
 * Copyright 2012-2013 the original author or authors.
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

/**
 * The configuration of a repository
 * 
 * @author Andy Wilkinson
 */
public final class RepositoryConfiguration {

	private final String name;

	private final URI uri;

	private final boolean snapshotsEnabled;

	/**
	 * Creates a new {@code RepositoryConfiguration}.
	 * 
	 * @param name The name of the repository
	 * @param uri The uri of the repository
	 * @param snapshotsEnabled {@code true} if the repository should enable access to
	 * snapshots, {@code false} otherwise
	 */
	public RepositoryConfiguration(String name, URI uri, boolean snapshotsEnabled) {
		this.name = name;
		this.uri = uri;
		this.snapshotsEnabled = snapshotsEnabled;
	}

	/**
	 * @return the name of the repository
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
	 * @return the uri of the repository
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * @return {@code true} if the repository should enable access to snapshots,
	 * {@code false} otherwise
	 */
	public boolean getSnapshotsEnabled() {
		return this.snapshotsEnabled;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryConfiguration other = (RepositoryConfiguration) obj;
		if (this.name == null) {
			if (other.name != null)
				return false;
		}
		else if (!this.name.equals(other.name))
			return false;
		return true;
	}

}