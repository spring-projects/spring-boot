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
public class SimpleConfigurationMetadataRepository
		implements ConfigurationMetadataRepository, Serializable {

	private final Map<String, ConfigurationMetadataGroup> allGroups = new HashMap<>();

	@Override
	public Map<String, ConfigurationMetadataGroup> getAllGroups() {
		return Collections.unmodifiableMap(this.allGroups);
	}

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
			ConfigurationMetadataGroup group = this.allGroups.get(groupId);
			if (group == null) {
				group = new ConfigurationMetadataGroup(groupId);
				this.allGroups.put(groupId, group);
			}
			String sourceType = source.getType();
			if (sourceType != null) {
				putIfAbsent(group.getSources(), sourceType, source);
			}
		}
	}

	/**
	 * Add a {@link ConfigurationMetadataProperty} with the
	 * {@link ConfigurationMetadataSource source} that defines it, if any.
	 * @param property the property to add
	 * @param source the source
	 */
	public void add(ConfigurationMetadataProperty property,
			ConfigurationMetadataSource source) {
		if (source != null) {
			putIfAbsent(source.getProperties(), property.getId(), property);
		}
		putIfAbsent(getGroup(source).getProperties(), property.getId(), property);
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
				for (Map.Entry<String, ConfigurationMetadataProperty> entry : group
						.getProperties().entrySet()) {
					putIfAbsent(existingGroup.getProperties(), entry.getKey(),
							entry.getValue());
				}
				// Merge sources
				for (Map.Entry<String, ConfigurationMetadataSource> entry : group
						.getSources().entrySet()) {
					putIfAbsent(existingGroup.getSources(), entry.getKey(),
							entry.getValue());
				}
			}
		}

	}

	private ConfigurationMetadataGroup getGroup(ConfigurationMetadataSource source) {
		if (source == null) {
			ConfigurationMetadataGroup rootGroup = this.allGroups.get(ROOT_GROUP);
			if (rootGroup == null) {
				rootGroup = new ConfigurationMetadataGroup(ROOT_GROUP);
				this.allGroups.put(ROOT_GROUP, rootGroup);
			}
			return rootGroup;
		}
		return this.allGroups.get(source.getGroupId());
	}

	private <V> void putIfAbsent(Map<String, V> map, String key, V value) {
		if (!map.containsKey(key)) {
			map.put(key, value);
		}
	}

}
