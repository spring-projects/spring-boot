/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties.migrator;

import java.util.Comparator;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.TextResourceOrigin;

/**
 * Description of a property migration.
 *
 * @author Stephane Nicoll
 */
class PropertyMigration {

	public static final Comparator<PropertyMigration> COMPARATOR = Comparator
			.comparing((property) -> property.getMetadata().getId());

	private final ConfigurationMetadataProperty metadata;

	private final ConfigurationProperty property;

	private final Integer lineNumber;

	PropertyMigration(ConfigurationMetadataProperty metadata,
			ConfigurationProperty property) {
		this.metadata = metadata;
		this.property = property;
		this.lineNumber = determineLineNumber(property);
	}

	private static Integer determineLineNumber(ConfigurationProperty property) {
		Origin origin = property.getOrigin();
		if (origin instanceof TextResourceOrigin) {
			TextResourceOrigin textOrigin = (TextResourceOrigin) origin;
			if (textOrigin.getLocation() != null) {
				return textOrigin.getLocation().getLine() + 1;
			}
		}
		return null;
	}

	public ConfigurationMetadataProperty getMetadata() {
		return this.metadata;
	}

	public ConfigurationProperty getProperty() {
		return this.property;
	}

	public Integer getLineNumber() {
		return this.lineNumber;
	}

}
