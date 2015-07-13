package org.springframework.boot.configurationmetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * A raw view of a hint used for parsing only.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class ConfigurationMetadataHint {

	private String id;

	private final List<ValueHint> valueHints = new ArrayList<ValueHint>();

	private final List<ValueProvider> valueProviders = new ArrayList<ValueProvider>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ValueHint> getValueHints() {
		return valueHints;
	}

	public List<ValueProvider> getValueProviders() {
		return valueProviders;
	}

}
