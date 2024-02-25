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

package org.springframework.boot.configurationmetadata;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The default {@link ConfigurationMetadataRepository} implementation.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class SimpleConfigurationMetadataRepository implements ConfigurationMetadataRepository, Serializable {

	private final Map<String, ConfigurationMetadataGroup> allGroups = new HashMap<>();

	/**
     * Returns an unmodifiable map of all configuration metadata groups.
     *
     * @return an unmodifiable map of all configuration metadata groups
     */
    @Override
	public Map<String, ConfigurationMetadataGroup> getAllGroups() {
		return Collections.unmodifiableMap(this.allGroups);
	}

	/**
     * Returns a map of all properties in the configuration metadata repository.
     * 
     * @return a map of property names to ConfigurationMetadataProperty objects
     */
    @Override
	public Map<String, ConfigurationMetadataProperty> getAllProperties() {
		Map<String, ConfigurationMetadataProperty> properties = new HashMap<>();
		for (ConfigurationMetadataGroup group : this.allGroups.values()) {
			properties.putAll(group.getProperties());
		}
		return properties;
	}

	/**
	 * Register the specified {@link ConfigurationMetadataSource sources}.
	 * @param sources the sources to add
	 */
	public void add(Collection<ConfigurationMetadataSource> sources) {
		for (ConfigurationMetadataSource source : sources) {
			String groupId = source.getGroupId();
			ConfigurationMetadataGroup group = this.allGroups.computeIfAbsent(groupId,
					(key) -> new ConfigurationMetadataGroup(groupId));
			String sourceType = source.getType();
			if (sourceType != null) {
				addOrMergeSource(group.getSources(), sourceType, source);
			}
		}
	}

	/**
	 * Add a {@link ConfigurationMetadataProperty} with the
	 * {@link ConfigurationMetadataSource source} that defines it, if any.
	 * @param property the property to add
	 * @param source the source
	 */
	public void add(ConfigurationMetadataProperty property, ConfigurationMetadataSource source) {
		if (source != null) {
			source.getProperties().putIfAbsent(property.getId(), property);
		}
		getGroup(source).getProperties().putIfAbsent(property.getId(), property);
	}

	/**
	 * Merge the content of the specified repository to this repository.
	 * @param repository the repository to include
	 */
	public void include(ConfigurationMetadataRepository repository) {
		for (ConfigurationMetadataGroup group : repository.getAllGroups().values()) {
			ConfigurationMetadataGroup existingGroup = this.allGroups.get(group.getId());
			if (existingGroup == null) {
				this.allGroups.put(group.getId(), group);
			}
			else {
				// Merge properties
				group.getProperties().forEach((name, value) -> existingGroup.getProperties().putIfAbsent(name, value));
				// Merge sources
				group.getSources().forEach((name, value) -> addOrMergeSource(existingGroup.getSources(), name, value));
			}
		}

	}

	/**
     * Retrieves the ConfigurationMetadataGroup associated with the given ConfigurationMetadataSource.
     * If the source is null, it returns the root ConfigurationMetadataGroup.
     * 
     * @param source the ConfigurationMetadataSource to retrieve the group for
     * @return the ConfigurationMetadataGroup associated with the source, or the root group if the source is null
     */
    private ConfigurationMetadataGroup getGroup(ConfigurationMetadataSource source) {
		if (source == null) {
			return this.allGroups.computeIfAbsent(ROOT_GROUP, (key) -> new ConfigurationMetadataGroup(ROOT_GROUP));
		}
		return this.allGroups.get(source.getGroupId());
	}

	/**
     * Adds or merges a configuration metadata source to the given map of sources.
     * If a source with the same name already exists in the map, the properties of the existing source are merged with the new source.
     * If no source with the same name exists, the new source is added to the map.
     * 
     * @param sources the map of configuration metadata sources
     * @param name the name of the configuration metadata source
     * @param source the configuration metadata source to be added or merged
     */
    private void addOrMergeSource(Map<String, ConfigurationMetadataSource> sources, String name,
			ConfigurationMetadataSource source) {
		ConfigurationMetadataSource existingSource = sources.get(name);
		if (existingSource == null) {
			sources.put(name, source);
		}
		else {
			source.getProperties().forEach((k, v) -> existingSource.getProperties().putIfAbsent(k, v));
		}
	}

}
