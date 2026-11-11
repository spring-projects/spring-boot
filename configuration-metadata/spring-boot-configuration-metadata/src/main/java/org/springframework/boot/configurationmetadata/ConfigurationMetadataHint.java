/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * A raw view of a hint used for parsing only.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataHint {

	private static final String KEY_SUFFIX = ".keys";

	private static final String VALUE_SUFFIX = ".values";

	private String id;

	private final List<ValueHint> valueHints = new ArrayList<>();

	private final List<ValueProvider> valueProviders = new ArrayList<>();

	boolean isMapKeyHints() {
		return (this.id != null && this.id.endsWith(KEY_SUFFIX));
	}

	boolean isMapValueHints() {
		return (this.id != null && this.id.endsWith(VALUE_SUFFIX));
	}

	String resolveId() {
		if (isMapKeyHints()) {
			return this.id.substring(0, this.id.length() - KEY_SUFFIX.length());
		}
		if (isMapValueHints()) {
			return this.id.substring(0, this.id.length() - VALUE_SUFFIX.length());
		}
		return this.id;
	}

	String getId() {
		return this.id;
	}

	void setId(String id) {
		this.id = id;
	}

	List<ValueHint> getValueHints() {
		return this.valueHints;
	}

	List<ValueProvider> getValueProviders() {
		return this.valueProviders;
	}

}
