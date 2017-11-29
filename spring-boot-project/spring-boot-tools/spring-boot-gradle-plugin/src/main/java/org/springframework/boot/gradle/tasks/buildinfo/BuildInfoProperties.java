/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;

/**
 * The properties that are written into the {@code build-info.properties} file.
 *
 * @author Andy Wilkinson
 */
public class BuildInfoProperties implements Serializable {

	private final transient Project project;

	private String group;

	private String artifact;

	private String version;

	private String name;

	private Map<String, Object> additionalProperties = new HashMap<>();

	BuildInfoProperties(Project project) {
		this.project = project;
	}

	/**
	 * Returns the value used for the {@code build.group} property. Defaults to the
	 * {@link Project#getGroup() Project's group}.
	 *
	 * @return the group
	 */
	public String getGroup() {
		return this.group != null ? this.group : this.project.getGroup().toString();
	}

	/**
	 * Sets the value used for the {@code build.group} property.
	 *
	 * @param group the group name
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Returns the value used for the {@code build.artifact} property.
	 *
	 * @return the artifact
	 */
	public String getArtifact() {
		return this.artifact;
	}

	/**
	 * Sets the value used for the {@code build.artifact} property.
	 *
	 * @param artifact the artifact
	 */
	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	/**
	 * Returns the value used for the {@code build.version} property. Defaults to the
	 * {@link Project#getVersion() Project's version}.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return this.version != null ? this.version : this.project.getVersion().toString();
	}

	/**
	 * Sets the value used for the {@code build.version} property.
	 *
	 * @param version the version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Returns the value used for the {@code build.name} property. Defaults to the
	 * {@link Project#getDisplayName() Project's display name}.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name != null ? this.name : this.project.getName();
	}

	/**
	 * Sets the value used for the {@code build.name} property.
	 *
	 * @param name the name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the additional properties that will be included. When written, the name of
	 * each additional property is prefixed with {@code build.}.
	 *
	 * @return the additional properties
	 */
	public Map<String, Object> getAdditional() {
		return this.additionalProperties;
	}

	/**
	 * Sets the additional properties that will be included. When written, the name of
	 * each additional property is prefixed with {@code build.}.
	 *
	 * @param additionalProperties the additional properties
	 */
	public void setAdditional(Map<String, Object> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.additionalProperties == null) ? 0
				: this.additionalProperties.hashCode());
		result = prime * result
				+ ((this.artifact == null) ? 0 : this.artifact.hashCode());
		result = prime * result + ((this.group == null) ? 0 : this.group.hashCode());
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = prime * result + ((this.version == null) ? 0 : this.version.hashCode());
		return result;
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
		BuildInfoProperties other = (BuildInfoProperties) obj;
		if (this.additionalProperties == null) {
			if (other.additionalProperties != null) {
				return false;
			}
		}
		else if (!this.additionalProperties.equals(other.additionalProperties)) {
			return false;
		}
		if (this.artifact == null) {
			if (other.artifact != null) {
				return false;
			}
		}
		else if (!this.artifact.equals(other.artifact)) {
			return false;
		}
		if (this.group == null) {
			if (other.group != null) {
				return false;
			}
		}
		else if (!this.group.equals(other.group)) {
			return false;
		}
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		}
		else if (!this.name.equals(other.name)) {
			return false;
		}
		if (this.version == null) {
			if (other.version != null) {
				return false;
			}
		}
		else if (!this.version.equals(other.version)) {
			return false;
		}
		return true;
	}

}
