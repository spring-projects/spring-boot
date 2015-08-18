/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

/**
 * A raw view of a hint used for parsing only.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class ConfigurationMetadataHint {

	private String id;

	private final List<ValueHint> valueHints = new ArrayList<ValueHint>();

	private final List<ValueProvider> valueProviders = new ArrayList<ValueProvider>();

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ValueHint> getValueHints() {
		return this.valueHints;
	}

	public List<ValueProvider> getValueProviders() {
		return this.valueProviders;
	}

}
