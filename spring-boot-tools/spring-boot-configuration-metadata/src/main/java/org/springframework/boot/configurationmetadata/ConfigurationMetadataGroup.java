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
import java.util.HashMap;
import java.util.Map;

/**
 * Gather a collection of {@link ConfigurationMetadataProperty properties} that are
 * sharing a {@link #getId() common prefix}. Provide access to all the
 * {@link ConfigurationMetadataSource sources} that have contributed properties to the
 * group.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class ConfigurationMetadataGroup implements Serializable {

	private final String id;

	private final Map<String, ConfigurationMetadataSource> sources = new HashMap<>();

	private final Map<String, ConfigurationMetadataProperty> properties = new HashMap<>();

	public ConfigurationMetadataGroup(String id) {
		this.id = id;
	}

	/**
	 * Return the id of the group, used as a common prefix for all properties associated
	 * to it.
	 * @return the id of the group
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Return the {@link ConfigurationMetadataSource sources} defining the properties of
	 * this group.
	 * @return the sources of the group
	 */
	public Map<String, ConfigurationMetadataSource> getSources() {
		return this.sources;
	}

	/**
	 * Return the {@link ConfigurationMetadataProperty properties} defined in this group.
	 * <p>
	 * A property may appear more than once for a given source, potentially with
	 * conflicting type or documentation. This is a "merged" view of the properties of
	 * this group.
	 * @return the properties of the group
	 * @see ConfigurationMetadataSource#getProperties()
	 */
	public Map<String, ConfigurationMetadataProperty> getProperties() {
		return this.properties;
	}

}
