/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

/**
 * A raw view of a hint used for parsing only.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataHint {

	private static final String KEY_SUFFIX = ".keys";

	private static final String VALUE_SUFFIX = ".values";

	private String id;

	private final List<ValueHint> valueHints = new ArrayList<>();

	private final List<ValueProvider> valueProviders = new ArrayList<>();

	/**
	 * Returns true if the id of the ConfigurationMetadataHint object is not null and ends
	 * with the specified key suffix.
	 * @return true if the id ends with the key suffix, false otherwise
	 */
	boolean isMapKeyHints() {
		return (this.id != null && this.id.endsWith(KEY_SUFFIX));
	}

	/**
	 * Returns true if the id of the ConfigurationMetadataHint ends with the value suffix.
	 * @return true if the id ends with the value suffix, false otherwise
	 */
	boolean isMapValueHints() {
		return (this.id != null && this.id.endsWith(VALUE_SUFFIX));
	}

	/**
	 * Resolves the ID by removing the suffix based on the hint type.
	 * @return the resolved ID
	 */
	String resolveId() {
		if (isMapKeyHints()) {
			return this.id.substring(0, this.id.length() - KEY_SUFFIX.length());
		}
		if (isMapValueHints()) {
			return this.id.substring(0, this.id.length() - VALUE_SUFFIX.length());
		}
		return this.id;
	}

	/**
	 * Returns the ID of the ConfigurationMetadataHint.
	 * @return the ID of the ConfigurationMetadataHint
	 */
	String getId() {
		return this.id;
	}

	/**
	 * Sets the ID of the ConfigurationMetadataHint.
	 * @param id the ID to set
	 */
	void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the list of value hints for this configuration metadata hint.
	 * @return the list of value hints
	 */
	List<ValueHint> getValueHints() {
		return this.valueHints;
	}

	/**
	 * Returns the list of value providers.
	 * @return the list of value providers
	 */
	List<ValueProvider> getValueProviders() {
		return this.valueProviders;
	}

}
