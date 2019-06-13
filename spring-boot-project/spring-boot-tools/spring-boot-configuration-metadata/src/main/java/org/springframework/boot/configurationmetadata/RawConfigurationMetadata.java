/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.configurationmetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A raw metadata structure. Used to initialize a {@link ConfigurationMetadataRepository}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
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

	public List<ConfigurationMetadataSource> getSources() {
		return this.sources;
	}

	public ConfigurationMetadataSource getSource(ConfigurationMetadataItem item) {
		if (item.getSourceType() == null) {
			return null;
		}
		return this.sources.stream()
				.filter((candidate) -> item.getSourceType().equals(candidate.getType())
						&& item.getId().startsWith(candidate.getGroupId()))
				.max(Comparator.comparingInt((candidate) -> candidate.getGroupId().length())).orElse(null);
	}

	public List<ConfigurationMetadataItem> getItems() {
		return this.items;
	}

	public List<ConfigurationMetadataHint> getHints() {
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
