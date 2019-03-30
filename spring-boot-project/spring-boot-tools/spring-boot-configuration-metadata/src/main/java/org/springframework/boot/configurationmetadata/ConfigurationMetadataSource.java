/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

/**
 * A source of configuration metadata. Also defines where the source is declared, for
 * instance if it is defined as a {@code @Bean}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class ConfigurationMetadataSource implements Serializable {

	private String groupId;

	private String type;

	private String description;

	private String shortDescription;

	private String sourceType;

	private String sourceMethod;

	private final Map<String, ConfigurationMetadataProperty> properties = new HashMap<>();

	/**
	 * The identifier of the group to which this source is associated.
	 * @return the group id
	 */
	public String getGroupId() {
		return this.groupId;
	}

	void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * The type of the source. Usually this is the fully qualified name of a class that
	 * defines configuration items. This class may or may not be available at runtime.
	 * @return the type
	 */
	public String getType() {
		return this.type;
	}

	void setType(String type) {
		this.type = type;
	}

	/**
	 * A description of this source, if any. Can be multi-lines.
	 * @return the description
	 * @see #getShortDescription()
	 */
	public String getDescription() {
		return this.description;
	}

	void setDescription(String description) {
		this.description = description;
	}

	/**
	 * A single-line, single-sentence description of this source, if any.
	 * @return the short description
	 * @see #getDescription()
	 */
	public String getShortDescription() {
		return this.shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * The type where this source is defined. This can be identical to the
	 * {@link #getType() type} if the source is self-defined.
	 * @return the source type
	 */
	public String getSourceType() {
		return this.sourceType;
	}

	void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * The method name that defines this source, if any.
	 * @return the source method
	 */
	public String getSourceMethod() {
		return this.sourceMethod;
	}

	void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	/**
	 * Return the properties defined by this source.
	 * @return the properties
	 */
	public Map<String, ConfigurationMetadataProperty> getProperties() {
		return this.properties;
	}

}
