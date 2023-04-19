/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import org.springframework.util.function.SingletonSupplier;

/**
 * The properties that are written into the {@code build-info.properties} file.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public abstract class BuildInfoProperties implements Serializable {

	private final SetProperty<String> excludes;

	private final Supplier<String> creationTime = SingletonSupplier.of(new CurrentIsoInstantSupplier());

	@Inject
	public BuildInfoProperties(Project project, SetProperty<String> excludes) {
		this.excludes = excludes;
		getGroup().convention(project.provider(() -> project.getGroup().toString()));
		getVersion().convention(project.provider(() -> project.getVersion().toString()));
		getArtifact()
			.convention(project.provider(() -> project.findProperty("archivesBaseName")).map(Object::toString));
		getName().convention(project.provider(project::getName));
	}

	/**
	 * Returns the {@code build.group} property. Defaults to the {@link Project#getGroup()
	 * Project's group}.
	 * @return the group property
	 */
	@Internal
	public abstract Property<String> getGroup();

	/**
	 * Returns the {@code build.artifact} property.
	 * @return the artifact property
	 */
	@Internal
	public abstract Property<String> getArtifact();

	/**
	 * Returns the {@code build.version} property. Defaults to the
	 * {@link Project#getVersion() Project's version}.
	 * @return the version
	 */
	@Internal
	public abstract Property<String> getVersion();

	/**
	 * Returns the {@code build.name} property. Defaults to the {@link Project#getName()
	 * Project's name}.
	 * @return the name
	 */
	@Internal
	public abstract Property<String> getName();

	/**
	 * Returns the {@code build.time} property.
	 * @return the time
	 */
	@Internal
	public abstract Property<String> getTime();

	/**
	 * Returns the additional properties that will be included. When written, the name of
	 * each additional property is prefixed with {@code build.}.
	 * @return the additional properties
	 */
	@Internal
	public abstract MapProperty<String, Object> getAdditional();

	@Input
	@Optional
	String getArtifactIfNotExcluded() {
		return getIfNotExcluded(getArtifact(), "artifact");
	}

	@Input
	@Optional
	String getGroupIfNotExcluded() {
		return getIfNotExcluded(getGroup(), "group");
	}

	@Input
	@Optional
	String getNameIfNotExcluded() {
		return getIfNotExcluded(getName(), "name");
	}

	@Input
	@Optional
	Instant getTimeIfNotExcluded() {
		String time = getIfNotExcluded(getTime(), "time", this.creationTime);
		return (time != null) ? Instant.parse(time) : null;
	}

	@Input
	@Optional
	String getVersionIfNotExcluded() {
		return getIfNotExcluded(getVersion(), "version");
	}

	@Input
	Map<String, String> getAdditionalIfNotExcluded() {
		return coerceToStringValues(applyExclusions(getAdditional().getOrElse(Collections.emptyMap())));
	}

	private <T> T getIfNotExcluded(Property<T> property, String name) {
		return getIfNotExcluded(property, name, () -> null);
	}

	private <T> T getIfNotExcluded(Property<T> property, String name, Supplier<T> defaultValue) {
		if (this.excludes.getOrElse(Collections.emptySet()).contains(name)) {
			return null;
		}
		return property.getOrElse(defaultValue.get());
	}

	private Map<String, String> coerceToStringValues(Map<String, Object> input) {
		Map<String, String> output = new HashMap<>();
		input.forEach((key, value) -> output.put(key, (value != null) ? value.toString() : null));
		return output;
	}

	private Map<String, Object> applyExclusions(Map<String, Object> input) {
		Map<String, Object> output = new HashMap<>();
		Set<String> exclusions = this.excludes.getOrElse(Collections.emptySet());
		input.forEach((key, value) -> output.put(key, (!exclusions.contains(key)) ? value : null));
		return output;
	}

	private static final class CurrentIsoInstantSupplier implements Supplier<String> {

		@Override
		public String get() {
			return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		}

	}

}
