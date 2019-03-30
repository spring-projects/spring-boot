/*
 * Copyright 2012-2018 the original author or authors.
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
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.util.StringUtils;

/**
 * Description of a property migration.
 *
 * @author Stephane Nicoll
 */
class PropertyMigration {

	public static final Comparator<PropertyMigration> COMPARATOR = Comparator
			.comparing((property) -> property.getMetadata().getId());

	private final ConfigurationProperty property;

	private final Integer lineNumber;

	private final ConfigurationMetadataProperty metadata;

	private final ConfigurationMetadataProperty replacementMetadata;

	private final boolean compatibleType;

	PropertyMigration(ConfigurationProperty property,
			ConfigurationMetadataProperty metadata,
			ConfigurationMetadataProperty replacementMetadata) {
		this.property = property;
		this.lineNumber = determineLineNumber(property);
		this.metadata = metadata;
		this.replacementMetadata = replacementMetadata;
		this.compatibleType = determineCompatibleType(metadata, replacementMetadata);
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

	private static boolean determineCompatibleType(ConfigurationMetadataProperty metadata,
			ConfigurationMetadataProperty replacementMetadata) {
		String currentType = metadata.getType();
		String replacementType = determineReplacementType(replacementMetadata);
		if (replacementType == null || currentType == null) {
			return false;
		}
		if (replacementType.equals(currentType)) {
			return true;
		}
		if (replacementType.equals(Duration.class.getName())
				&& (currentType.equals(Long.class.getName())
						|| currentType.equals(Integer.class.getName()))) {
			return true;
		}
		return false;
	}

	private static String determineReplacementType(
			ConfigurationMetadataProperty replacementMetadata) {
		if (replacementMetadata == null || replacementMetadata.getType() == null) {
			return null;
		}
		String candidate = replacementMetadata.getType();
		if (candidate.startsWith(Map.class.getName())) {
			int lastComma = candidate.lastIndexOf(',');
			if (lastComma != -1) {
				return candidate.substring(lastComma + 1, candidate.length() - 1).trim();
			}
		}
		return candidate;
	}

	public ConfigurationProperty getProperty() {
		return this.property;
	}

	public Integer getLineNumber() {
		return this.lineNumber;
	}

	public ConfigurationMetadataProperty getMetadata() {
		return this.metadata;
	}

	public boolean isCompatibleType() {
		return this.compatibleType;
	}

	public String determineReason() {
		if (this.compatibleType) {
			return "Replacement: " + this.metadata.getDeprecation().getReplacement();
		}
		Deprecation deprecation = this.metadata.getDeprecation();
		if (StringUtils.hasText(deprecation.getShortReason())) {
			return "Reason: " + deprecation.getShortReason();
		}
		if (StringUtils.hasText(deprecation.getReplacement())) {
			if (this.replacementMetadata != null) {
				return String.format(
						"Reason: Replacement key '%s' uses an incompatible target type",
						deprecation.getReplacement());
			}
			else {
				return String.format("Reason: No metadata found for replacement key '%s'",
						deprecation.getReplacement());
			}
		}
		return "Reason: none";
	}

}
