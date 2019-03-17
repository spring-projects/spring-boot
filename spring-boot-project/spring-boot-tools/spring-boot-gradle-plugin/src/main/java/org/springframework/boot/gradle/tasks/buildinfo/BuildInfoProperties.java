/*
 * Copyright 2012-2018 the original author or authors.
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;

/**
 * The properties that are written into the {@code build-info.properties} file.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public class BuildInfoProperties implements Serializable {

	private final transient Project project;

	private String group;

	private String artifact;

	private String version;

	private String name;

	private Instant time;

	private Map<String, Object> additionalProperties = new HashMap<>();

	BuildInfoProperties(Project project) {
		this.project = project;
		this.time = Instant.now();
	}

	/**
	 * Returns the value used for the {@code build.group} property. Defaults to the
	 * {@link Project#getGroup() Project's group}.
	 * @return the group
	 */
	public String getGroup() {
		if (this.group == null) {
			this.group = this.project.getGroup().toString();
		}
		return this.group;
	}

	/**
	 * Sets the value used for the {@code build.group} property.
	 * @param group the group name
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Returns the value used for the {@code build.artifact} property.
	 * @return the artifact
	 */
	public String getArtifact() {
		return this.artifact;
	}

	/**
	 * Sets the value used for the {@code build.artifact} property.
	 * @param artifact the artifact
	 */
	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	/**
	 * Returns the value used for the {@code build.version} property. Defaults to the
	 * {@link Project#getVersion() Project's version}.
	 * @return the version
	 */
	public String getVersion() {
		if (this.version == null) {
			this.version = this.project.getVersion().toString();
		}
		return this.version;
	}

	/**
	 * Sets the value used for the {@code build.version} property.
	 * @param version the version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Returns the value used for the {@code build.name} property. Defaults to the
	 * {@link Project#getDisplayName() Project's display name}.
	 * @return the name
	 */
	public String getName() {
		if (this.name == null) {
			this.name = this.project.getName();
		}
		return this.name;
	}

	/**
	 * Sets the value used for the {@code build.name} property.
	 * @param name the name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the value used for the {@code build.time} property. Defaults to
	 * {@link Instant#now} when the {@code BuildInfoProperties} instance was created.
	 * @return the time
	 */
	public Instant getTime() {
		return this.time;
	}

	/**
	 * Sets the value used for the {@code build.time} property.
	 * @param time the build time
	 */
	public void setTime(Instant time) {
		this.time = time;
	}

	/**
	 * Returns the additional properties that will be included. When written, the name of
	 * each additional property is prefixed with {@code build.}.
	 * @return the additional properties
	 */
	public Map<String, Object> getAdditional() {
		return this.additionalProperties;
	}

	/**
	 * Sets the additional properties that will be included. When written, the name of
	 * each additional property is prefixed with {@code build.}.
	 * @param additionalProperties the additional properties
	 */
	public void setAdditional(Map<String, Object> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		BuildInfoProperties other = (BuildInfoProperties) obj;
		boolean result = true;
		result = result
				&& nullSafeEquals(this.additionalProperties, other.additionalProperties);
		result = result && nullSafeEquals(this.artifact, other.artifact);
		result = result && nullSafeEquals(this.group, other.group);
		result = result && nullSafeEquals(this.name, other.name);
		result = result && nullSafeEquals(this.version, other.version);
		result = result && nullSafeEquals(this.time, other.time);
		return result;
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return (o1.equals(o2));
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
		result = prime * result + ((this.time == null) ? 0 : this.time.hashCode());
		return result;
	}

}
