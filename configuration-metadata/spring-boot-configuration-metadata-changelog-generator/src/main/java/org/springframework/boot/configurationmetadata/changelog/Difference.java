/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata.changelog;

import java.util.Objects;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * A difference in the metadata.
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
			Level level = newProperty.getDeprecation().getLevel();
			DifferenceType differenceType = (level == Level.WARNING) ? DifferenceType.DEPRECATED
					: DifferenceType.DELETED;
			return new Difference(differenceType, oldProperty, newProperty);
		}
		if (oldProperty.isDeprecated() && oldProperty.getDeprecation().getLevel() == Level.WARNING
				&& newProperty.isDeprecated() && newProperty.getDeprecation().getLevel() == Level.ERROR) {
			return new Difference(DifferenceType.DELETED, oldProperty, newProperty);
		}
		if (!Objects.deepEquals(oldProperty.getDefaultValue(), newProperty.getDefaultValue())) {
			return new Difference(DifferenceType.DEFAULT_CHANGED, oldProperty, newProperty);
		}
		return null;
	}

}
