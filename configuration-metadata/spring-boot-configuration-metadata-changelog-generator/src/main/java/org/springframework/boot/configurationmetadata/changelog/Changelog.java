/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata.changelog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * A changelog containing differences computed from two repositories of configuration
 * metadata.
 *
 * @param oldVersionNumber the name of the old version
 * @param newVersionNumber the name of the new version
 * @param differences the differences
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Yoobin Yoon
 * @author Junggi Kim
 */
record Changelog(String oldVersionNumber, String newVersionNumber, List<Difference> differences) {

	static Changelog of(String oldVersionNumber, ConfigurationMetadataRepository oldMetadata, String newVersionNumber,
			ConfigurationMetadataRepository newMetadata) {
		return new Changelog(oldVersionNumber, newVersionNumber, computeDifferences(oldMetadata, newMetadata));
	}

	static List<Difference> computeDifferences(ConfigurationMetadataRepository oldMetadata,
			ConfigurationMetadataRepository newMetadata) {
		Map<String, ConfigurationMetadataProperty> oldProperties = oldMetadata.getAllProperties();
		Map<String, ConfigurationMetadataProperty> newProperties = newMetadata.getAllProperties();
		List<Difference> differences = new ArrayList<>();
		for (ConfigurationMetadataProperty oldProperty : oldProperties.values()) {
			ConfigurationMetadataProperty newProperty = newProperties.get(oldProperty.getId());
			Difference difference = Difference.compute(oldProperty, newProperty);
			if (difference != null) {
				differences.add(difference);
			}
		}
		for (ConfigurationMetadataProperty newProperty : newProperties.values()) {
			if (!oldProperties.containsKey(newProperty.getId())) {
				if (!newProperty.isDeprecated()) {
					differences.add(new Difference(DifferenceType.ADDED, null, newProperty));
				}
				else {
					DifferenceType differenceType = (newProperty.getDeprecation().getLevel() == Level.ERROR)
							? DifferenceType.DELETED : DifferenceType.ADDED;
					differences.add(new Difference(differenceType, null, newProperty));
				}
			}
		}
		return List.copyOf(differences);
	}

}
