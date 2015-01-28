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

package org.springframework.boot.configurationprocessor;

import javax.annotation.processing.RoundEnvironment;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Standard implementation of {@code BuildHandler} that handles the state of a single
 * build.
 *
 * @author Andy Wilkinson
 * @since 1.2.2
 */
public class StandardBuildHandler implements BuildHandler {

	private final ConfigurationMetadata metadata = new ConfigurationMetadata();

	@Override
	public void addGroup(String name, String type, String sourceType, String sourceMethod) {
		this.metadata.add(ItemMetadata.newGroup(name, type, sourceType, sourceMethod));
	}

	@Override
	public void addProperty(String prefix, String name, String type, String sourceType,
			String sourceMethod, String description, Object defaultValue,
			boolean deprecated) {
		this.metadata.add(ItemMetadata.newProperty(prefix, name, type, sourceType,
				sourceMethod, description, defaultValue, deprecated));
	}

	@Override
	public void processing(RoundEnvironment environment) {

	}

	@Override
	public ConfigurationMetadata produceMetadata() {
		return this.metadata;
	}

}
