/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata;

import java.util.Map;

/**
 * A repository of configuration metadata.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public interface ConfigurationMetadataRepository {

	/**
	 * Defines the name of the "root" group, that is the group that gathers all the
	 * properties that aren't attached to a specific group.
	 */
	String ROOT_GROUP = "_ROOT_GROUP_";

	/**
	 * Return the groups, indexed by id.
	 * @return all configuration meta-data groups
	 */
	Map<String, ConfigurationMetadataGroup> getAllGroups();

	/**
	 * Return the properties, indexed by id.
	 * @return all configuration meta-data properties
	 */
	Map<String, ConfigurationMetadataProperty> getAllProperties();

}
