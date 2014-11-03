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

import java.util.Map;

/**
 * A repository of configuration metadata.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public interface ConfigurationMetadataRepository {

	/**
	 * Defines the name of the "root" group, that is the group that
	 * gathers all the properties that aren't attached to a specific
	 * group.
	 */
	static final String ROOT_GROUP = "_ROOT_GROUP_";

	/**
	 * Return the groups, indexed by id.
	 */
	Map<String, ConfigurationMetadataGroup> getAllGroups();

	/**
	 * Return the properties, indexed by id.
	 */
	Map<String, ConfigurationMetadataProperty> getAllProperties();

}
