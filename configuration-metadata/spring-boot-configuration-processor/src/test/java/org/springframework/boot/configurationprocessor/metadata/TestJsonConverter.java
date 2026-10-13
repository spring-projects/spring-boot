/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.metadata;

import java.util.Collection;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * {@link JsonConverter} for use in tests.
 *
 * @author Phillip Webb
 */
public class TestJsonConverter extends JsonConverter {

	@Override
	public JSONArray toJsonArray(ConfigurationMetadata metadata, ItemType itemType) throws Exception {
		return super.toJsonArray(metadata, itemType);
	}

	@Override
	public JSONArray toJsonArray(Collection<ItemHint> hints) throws Exception {
		return super.toJsonArray(hints);
	}

	@Override
	public JSONObject toJsonObject(ItemMetadata item) throws Exception {
		return super.toJsonObject(item);
	}

}
