/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A raw metadata structure. Used to initialize a {@link ConfigurationMetadataRepository}.
 *
 * @author Stephane Nicoll
 */
class RawConfigurationMetadata {

	private final List<ConfigurationMetadataSource> sources;

	private final List<ConfigurationMetadataItem> items;

	private final List<ConfigurationMetadataHint> hints;

	RawConfigurationMetadata(List<ConfigurationMetadataSource> sources, List<ConfigurationMetadataItem> items,
			List<ConfigurationMetadataHint> hints) {
		this.sources = new ArrayList<>(sources);
		this.items = new ArrayList<>(items);
		this.hints = new ArrayList<>(hints);
		for (ConfigurationMetadataItem item : this.items) {
			resolveName(item);
		}
	}

	List<ConfigurationMetadataSource> getSources() {
		return this.sources;
	}

	ConfigurationMetadataSource getSource(ConfigurationMetadataItem item) {
		if (item.getSourceType() == null) {
			return null;
		}
		return this.sources.stream()
			.filter((candidate) -> item.getSourceType().equals(candidate.getType())
					&& item.getId().startsWith(candidate.getGroupId()))
			.max(Comparator.comparingInt((candidate) -> candidate.getGroupId().length()))
			.orElse(null);
	}

	List<ConfigurationMetadataItem> getItems() {
		return this.items;
	}

	List<ConfigurationMetadataHint> getHints() {
		return this.hints;
	}

	/**
	 * Resolve the name of an item against this instance.
	 * @param item the item to resolve
	 * @see ConfigurationMetadataProperty#setName(String)
	 */
	private void resolveName(ConfigurationMetadataItem item) {
		item.setName(item.getId()); // fallback
		ConfigurationMetadataSource source = getSource(item);
		if (source != null) {
			String groupId = source.getGroupId();
			String dottedPrefix = groupId + ".";
			String id = item.getId();
			if (hasLength(groupId) && id.startsWith(dottedPrefix)) {
				String name = id.substring(dottedPrefix.length());
				item.setName(name);
			}
		}
	}

	private static boolean hasLength(String string) {
		return (string != null && !string.isEmpty());
	}

}
