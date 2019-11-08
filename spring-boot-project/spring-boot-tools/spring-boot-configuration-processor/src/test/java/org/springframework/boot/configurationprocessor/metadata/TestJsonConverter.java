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
