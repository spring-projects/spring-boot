/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.configurationmetadata.changelog;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * A difference the metadata.
 *
 * @param type the type of the difference
 * @param oldProperty the old property
 * @param newProperty the new property
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record Difference(DifferenceType type, ConfigurationMetadataProperty oldProperty,
		ConfigurationMetadataProperty newProperty) {

	static Difference compute(ConfigurationMetadataProperty oldProperty, ConfigurationMetadataProperty newProperty) {
		if (newProperty == null) {
			if (!(oldProperty.isDeprecated() && oldProperty.getDeprecation().getLevel() == Level.ERROR)) {
				return new Difference(DifferenceType.DELETED, oldProperty, null);
			}
			return null;
		}
		if (newProperty.isDeprecated() && !oldProperty.isDeprecated()) {
			return new Difference(DifferenceType.DEPRECATED, oldProperty, newProperty);
		}
		if (oldProperty.isDeprecated() && oldProperty.getDeprecation().getLevel() == Level.WARNING
				&& newProperty.isDeprecated() && newProperty.getDeprecation().getLevel() == Level.ERROR) {
			return new Difference(DifferenceType.DELETED, oldProperty, newProperty);
		}
		return null;
	}

}
