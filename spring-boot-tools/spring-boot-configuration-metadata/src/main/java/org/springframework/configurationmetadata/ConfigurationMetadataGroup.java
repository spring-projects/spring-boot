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

package org.springframework.configurationmetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Gather a collection of {@link ConfigurationMetadataProperty} that are sharing
 * a common prefix.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigurationMetadataGroup {

	private final String id;

	private final Map<String, ConfigurationMetadataSource> sources =
			new HashMap<String, ConfigurationMetadataSource>();

	private final Map<String, ConfigurationMetadataProperty> properties =
			new HashMap<String, ConfigurationMetadataProperty>();

	public ConfigurationMetadataGroup(String id) {
		this.id = id;
	}

	/**
	 * Return the id of the group.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Return the {@link ConfigurationMetadataSource sources} defining
	 * the properties of this group.
	 */
	public Map<String, ConfigurationMetadataSource> getSources() {
		return this.sources;
	}

	/**
	 * Return the {@link ConfigurationMetadataProperty properties} defined in this group.
	 * <p>A property may appear more than once for a given source, potentially with conflicting
	 * type or documentation. This is a "merged" view of the properties of this group.
	 * @see ConfigurationMetadataSource#getProperties()
	 */
	public Map<String, ConfigurationMetadataProperty> getProperties() {
		return this.properties;
	}

}
