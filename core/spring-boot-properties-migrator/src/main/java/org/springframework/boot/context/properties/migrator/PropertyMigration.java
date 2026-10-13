/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.migrator;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
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

	private final @Nullable Integer lineNumber;

	private final ConfigurationMetadataProperty metadata;

	private final @Nullable ConfigurationMetadataProperty replacementMetadata;

	/**
	 * Whether this migration happened from a map type.
	 */
	private final boolean mapMigration;

	private final boolean compatibleType;

	PropertyMigration(ConfigurationProperty property, ConfigurationMetadataProperty metadata,
			@Nullable ConfigurationMetadataProperty replacementMetadata, boolean mapMigration) {
		this.property = property;
		this.lineNumber = determineLineNumber(property);
		this.metadata = metadata;
		this.replacementMetadata = replacementMetadata;
		this.mapMigration = mapMigration;
		this.compatibleType = determineCompatibleType(metadata, replacementMetadata);
	}

	private static @Nullable Integer determineLineNumber(ConfigurationProperty property) {
		Origin origin = property.getOrigin();
		if (origin instanceof PropertySourceOrigin propertySourceOrigin) {
			origin = propertySourceOrigin.getOrigin();
		}
		if (origin instanceof TextResourceOrigin textOrigin) {
			if (textOrigin.getLocation() != null) {
				return textOrigin.getLocation().getLine() + 1;
			}
		}
		return null;
	}

	private static boolean determineCompatibleType(ConfigurationMetadataProperty metadata,
			@Nullable ConfigurationMetadataProperty replacementMetadata) {
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

	private static @Nullable String determineType(@Nullable ConfigurationMetadataProperty metadata) {
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

	ConfigurationProperty getProperty() {
		return this.property;
	}

	@Nullable Integer getLineNumber() {
		return this.lineNumber;
	}

	ConfigurationMetadataProperty getMetadata() {
		return this.metadata;
	}

	boolean isCompatibleType() {
		return this.compatibleType;
	}

	String getNewPropertyName() {
		if (this.mapMigration) {
			ConfigurationMetadataProperty replacementMetadata = this.replacementMetadata;
			Assert.state(replacementMetadata != null, "'replacementMetadata' must not be null");
			return getNewMapPropertyName(this.property, this.metadata, replacementMetadata).toString();
		}
		return this.metadata.getDeprecation().getReplacement();
	}

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

	private static ConfigurationPropertyName getNewMapPropertyName(ConfigurationProperty property,
			ConfigurationMetadataProperty metadata, ConfigurationMetadataProperty replacement) {
		ConfigurationPropertyName oldName = property.getName();
		ConfigurationPropertyName oldPrefix = ConfigurationPropertyName.of(metadata.getId());
		Assert.state(oldPrefix.isAncestorOf(oldName),
				() -> "'%s' is not an ancestor of '%s'".formatted(oldPrefix, oldName));
		ConfigurationPropertyName newPrefix = ConfigurationPropertyName.of(replacement.getId());
		return newPrefix.append(oldName.subName(oldPrefix.getNumberOfElements()));
	}

}
