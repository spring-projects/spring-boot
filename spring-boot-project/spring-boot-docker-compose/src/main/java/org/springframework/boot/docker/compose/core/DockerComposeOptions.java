/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.style.ToStringCreator;

/**
 * Default {@link DockerCompose.Options} implementation.
 *
 * @author Dmytro Nosan
 */
final class DockerComposeOptions implements DockerCompose.Options {

	private final DockerComposeFile composeFile;

	private final Set<String> activeProfiles;

	private final List<String> arguments;

	/**
	 * Create a new {@link DockerComposeOptions} instance.
	 * @param composeFile the Docker Compose file to use
	 * @param activeProfiles the Docker Compose profiles to activate
	 * @param arguments the additional Docker Compose arguments (e.g. --project-name=...)
	 */
	DockerComposeOptions(DockerComposeFile composeFile, Set<String> activeProfiles, List<String> arguments) {
		this.composeFile = composeFile;
		this.activeProfiles = (activeProfiles != null) ? activeProfiles : Collections.emptySet();
		this.arguments = (arguments != null) ? arguments : Collections.emptyList();
	}

	@Override
	public DockerComposeFile getComposeFile() {
		return this.composeFile;
	}

	@Override
	public Set<String> getActiveProfiles() {
		return this.activeProfiles;
	}

	@Override
	public List<String> getArguments() {
		return this.arguments;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DockerComposeOptions that = (DockerComposeOptions) obj;
		return Objects.equals(this.composeFile, that.composeFile)
				&& Objects.equals(this.activeProfiles, that.activeProfiles)
				&& Objects.equals(this.arguments, that.arguments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.composeFile, this.activeProfiles, this.arguments);
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("composeFile", this.composeFile);
		creator.append("activeProfiles", this.activeProfiles);
		creator.append("arguments", this.arguments);
		return creator.toString();
	}

}
