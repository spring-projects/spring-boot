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
 * A source of configuration metadata. Also defines where
 * the source is declared, for instance if it is defined
 * as a {@code @Bean}.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigurationMetadataSource {

	private String groupId;

	private String type;

	private String description;

	private String sourceType;

	private String sourceMethod;

	private final Map<String, ConfigurationMetadataProperty> properties
			= new HashMap<String, ConfigurationMetadataProperty>();

	/**
	 * The identifier of the group to which this source is associated
	 */
	public String getGroupId() {
		return this.groupId;
	}

	void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * The type of the source. Usually this is the fully qualified name
	 * of a class that defines one more configuration item. This class may or
	 * may not be available at runtime.
	 */
	public String getType() {
		return this.type;
	}

	void setType(String type) {
		this.type = type;
	}

	/**
	 * The description of this source, if any.
	 */
	public String getDescription() {
		return this.description;
	}

	void setDescription(String description) {
		this.description = description;
	}

	/**
	 * The type where this source is defined. This can be identical
	 * to the {@linkplain #getType() type} if the source is self-defined.
	 */
	public String getSourceType() {
		return this.sourceType;
	}

	void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * The method name that defines this source, if any.
	 */
	public String getSourceMethod() {
		return this.sourceMethod;
	}

	void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	/**
	 * Return the properties defined by this source.
	 */
	public Map<String, ConfigurationMetadataProperty> getProperties() {
		return this.properties;
	}

}
