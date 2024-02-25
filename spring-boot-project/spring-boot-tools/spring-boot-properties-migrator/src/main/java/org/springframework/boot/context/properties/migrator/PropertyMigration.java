/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.context.properties.migrator;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Description of a property migration.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class PropertyMigration {

	public static final Comparator<PropertyMigration> COMPARATOR = Comparator
		.comparing((property) -> property.getMetadata().getId());

	private final ConfigurationProperty property;

	private final Integer lineNumber;

	private final ConfigurationMetadataProperty metadata;

	private final ConfigurationMetadataProperty replacementMetadata;

	/**
	 * Whether this migration happened from a map type.
	 */
	private final boolean mapMigration;

	private final boolean compatibleType;

	/**
     * Constructs a new PropertyMigration object with the given parameters.
     * 
     * @param property            the ConfigurationProperty to be migrated
     * @param metadata            the ConfigurationMetadataProperty of the property
     * @param replacementMetadata the ConfigurationMetadataProperty of the replacement property
     * @param mapMigration        a boolean indicating whether the migration involves mapping
     * @return                    a new PropertyMigration object
     */
    PropertyMigration(ConfigurationProperty property, ConfigurationMetadataProperty metadata,
			ConfigurationMetadataProperty replacementMetadata, boolean mapMigration) {
		this.property = property;
		this.lineNumber = determineLineNumber(property);
		this.metadata = metadata;
		this.replacementMetadata = replacementMetadata;
		this.mapMigration = mapMigration;
		this.compatibleType = determineCompatibleType(metadata, replacementMetadata);
	}

	/**
     * Determines the line number of the given ConfigurationProperty's origin.
     * 
     * @param property the ConfigurationProperty to determine the line number for
     * @return the line number of the origin, or null if it cannot be determined
     */
    private static Integer determineLineNumber(ConfigurationProperty property) {
		Origin origin = property.getOrigin();
		if (origin instanceof TextResourceOrigin textOrigin) {
			if (textOrigin.getLocation() != null) {
				return textOrigin.getLocation().getLine() + 1;
			}
		}
		return null;
	}

	/**
     * Determines if the replacement metadata is compatible with the current metadata type.
     * 
     * @param metadata the current metadata property
     * @param replacementMetadata the replacement metadata property
     * @return true if the replacement metadata is compatible with the current metadata type, false otherwise
     */
    private static boolean determineCompatibleType(ConfigurationMetadataProperty metadata,
			ConfigurationMetadataProperty replacementMetadata) {
		String currentType = determineType(metadata);
		String replacementType = determineType(replacementMetadata);
		if (currentType == null || replacementType == null) {
			return false;
		}
		if (replacementType.equals(currentType)) {
			return true;
		}
		return replacementType.equals(Duration.class.getName())
				&& (currentType.equals(Long.class.getName()) || currentType.equals(Integer.class.getName()));
	}

	/**
     * Determines the type of a ConfigurationMetadataProperty.
     * 
     * @param metadata the ConfigurationMetadataProperty to determine the type for
     * @return the type of the ConfigurationMetadataProperty, or null if the metadata or type is null
     */
    private static String determineType(ConfigurationMetadataProperty metadata) {
		if (metadata == null || metadata.getType() == null) {
			return null;
		}
		String candidate = metadata.getType();
		if (candidate.startsWith(Map.class.getName())) {
			int lastComma = candidate.lastIndexOf(',');
			if (lastComma != -1) {
				// Use Map value type
				return candidate.substring(lastComma + 1, candidate.length() - 1).trim();
			}
		}
		return candidate;
	}

	/**
     * Retrieves the property associated with this PropertyMigration instance.
     * 
     * @return the property associated with this PropertyMigration instance
     */
    ConfigurationProperty getProperty() {
		return this.property;
	}

	/**
     * Returns the line number of the property migration.
     *
     * @return the line number of the property migration
     */
    Integer getLineNumber() {
		return this.lineNumber;
	}

	/**
     * Returns the metadata of the ConfigurationMetadataProperty.
     *
     * @return the metadata of the ConfigurationMetadataProperty
     */
    ConfigurationMetadataProperty getMetadata() {
		return this.metadata;
	}

	/**
     * Returns a boolean value indicating whether the type is compatible.
     *
     * @return true if the type is compatible, false otherwise.
     */
    boolean isCompatibleType() {
		return this.compatibleType;
	}

	/**
     * Returns the new property name based on the migration rules.
     * If map migration is enabled, it calls the getNewMapPropertyName method
     * to determine the new property name based on the given property, metadata, and replacement metadata.
     * Otherwise, it returns the replacement property name specified in the deprecation metadata.
     *
     * @return the new property name
     */
    String getNewPropertyName() {
		if (this.mapMigration) {
			return getNewMapPropertyName(this.property, this.metadata, this.replacementMetadata).toString();
		}
		return this.metadata.getDeprecation().getReplacement();
	}

	/**
     * Determines the reason for property migration.
     * 
     * @return the reason for property migration
     */
    String determineReason() {
		if (this.compatibleType) {
			return "Replacement: " + getNewPropertyName();
		}
		Deprecation deprecation = this.metadata.getDeprecation();
		if (StringUtils.hasText(deprecation.getShortReason())) {
			return "Reason: " + deprecation.getShortReason();
		}
		if (StringUtils.hasText(deprecation.getReplacement())) {
			if (this.replacementMetadata != null) {
				return String.format("Reason: Replacement key '%s' uses an incompatible target type",
						deprecation.getReplacement());
			}
			return String.format("Reason: No metadata found for replacement key '%s'", deprecation.getReplacement());
		}
		return "Reason: none";
	}

	/**
     * Returns the new ConfigurationPropertyName for a given ConfigurationProperty, ConfigurationMetadataProperty,
     * and replacement ConfigurationMetadataProperty.
     *
     * @param property   the ConfigurationProperty to get the old name from
     * @param metadata   the ConfigurationMetadataProperty to get the old prefix from
     * @param replacement   the ConfigurationMetadataProperty to get the new prefix from
     * @return the new ConfigurationPropertyName
     * @throws IllegalArgumentException if the old prefix is not an ancestor of the old name
     */
    private static ConfigurationPropertyName getNewMapPropertyName(ConfigurationProperty property,
			ConfigurationMetadataProperty metadata, ConfigurationMetadataProperty replacement) {
		ConfigurationPropertyName oldName = property.getName();
		ConfigurationPropertyName oldPrefix = ConfigurationPropertyName.of(metadata.getId());
		Assert.state(oldPrefix.isAncestorOf(oldName),
				String.format("'%s' is not an ancestor of '%s'", oldPrefix, oldName));
		ConfigurationPropertyName newPrefix = ConfigurationPropertyName.of(replacement.getId());
		return newPrefix.append(oldName.subName(oldPrefix.getNumberOfElements()));
	}

}
