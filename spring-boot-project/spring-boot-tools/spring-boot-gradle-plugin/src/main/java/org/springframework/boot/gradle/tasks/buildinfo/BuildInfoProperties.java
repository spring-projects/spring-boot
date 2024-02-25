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
import org.gradle.api.provider.Provider;
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

	/**
	 * Constructs a new instance of BuildInfoProperties.
	 * @param project the project object
	 * @param excludes the set of excludes
	 */
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

	/**
	 * Returns the artifact if it is not excluded.
	 * @return the artifact if it is not excluded, or null if it is excluded
	 */
	@Input
	@Optional
	String getArtifactIfNotExcluded() {
		return getIfNotExcluded(getArtifact(), "artifact");
	}

	/**
	 * Returns the group if it is not excluded.
	 * @return the group if it is not excluded, or null if it is excluded
	 */
	@Input
	@Optional
	String getGroupIfNotExcluded() {
		return getIfNotExcluded(getGroup(), "group");
	}

	/**
	 * Returns the name if it is not excluded.
	 * @return the name if it is not excluded, or null if it is excluded.
	 */
	@Input
	@Optional
	String getNameIfNotExcluded() {
		return getIfNotExcluded(getName(), "name");
	}

	/**
	 * Returns the time if it is not excluded.
	 * @return the time as an Instant object if it is not excluded, null otherwise
	 */
	@Input
	@Optional
	Instant getTimeIfNotExcluded() {
		String time = getIfNotExcluded(getTime(), "time", this.creationTime);
		return (time != null) ? Instant.parse(time) : null;
	}

	/**
	 * Returns the version if it is not excluded.
	 * @return the version if it is not excluded, or null if it is excluded
	 */
	@Input
	@Optional
	String getVersionIfNotExcluded() {
		return getIfNotExcluded(getVersion(), "version");
	}

	/**
	 * Returns a map of additional properties if they are not excluded.
	 * @return a map of additional properties if they are not excluded
	 */
	@Input
	Map<String, String> getAdditionalIfNotExcluded() {
		return coerceToStringValues(applyExclusions(getAdditional().getOrElse(Collections.emptyMap())));
	}

	/**
	 * Retrieves the value of the specified property if it is not excluded.
	 * @param <T> the type of the property value
	 * @param property the property to retrieve the value from
	 * @param name the name of the property
	 * @return the value of the property if it is not excluded, otherwise null
	 */
	private <T> T getIfNotExcluded(Property<T> property, String name) {
		return getIfNotExcluded(property, name, () -> null);
	}

	/**
	 * Retrieves the value of the specified property if it is not excluded.
	 * @param property the property to retrieve the value from
	 * @param name the name of the property
	 * @param defaultValue a supplier function to provide a default value if the property
	 * is not set
	 * @return the value of the property if it is not excluded, or null if it is excluded
	 */
	private <T> T getIfNotExcluded(Property<T> property, String name, Supplier<T> defaultValue) {
		if (this.excludes.getOrElse(Collections.emptySet()).contains(name)) {
			return null;
		}
		return property.getOrElse(defaultValue.get());
	}

	/**
	 * Coerces the values of a given map from Object type to String type.
	 * @param input the map containing the values to be coerced
	 * @return a new map with the coerced values
	 */
	private Map<String, String> coerceToStringValues(Map<String, Object> input) {
		Map<String, String> output = new HashMap<>();
		input.forEach((key, value) -> {
			if (value instanceof Provider<?> provider) {
				value = provider.getOrNull();
			}
			output.put(key, (value != null) ? value.toString() : null);
		});
		return output;
	}

	/**
	 * Applies exclusions to the input map and returns a new map with excluded values set
	 * to null.
	 * @param input the input map to apply exclusions to
	 * @return a new map with excluded values set to null
	 */
	private Map<String, Object> applyExclusions(Map<String, Object> input) {
		Map<String, Object> output = new HashMap<>();
		Set<String> exclusions = this.excludes.getOrElse(Collections.emptySet());
		input.forEach((key, value) -> output.put(key, (!exclusions.contains(key)) ? value : null));
		return output;
	}

	/**
	 * CurrentIsoInstantSupplier class.
	 */
	private static final class CurrentIsoInstantSupplier implements Supplier<String> {

		/**
		 * Returns the current ISO instant as a string.
		 * @return the current ISO instant formatted as a string
		 */
		@Override
		public String get() {
			return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		}

	}

}
