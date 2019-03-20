/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Thin wrapper to adapt {@link Gson} to a {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @since 1.2.0
 * @see JsonParserFactory
 */
public class GsonJsonParser implements JsonParser {

	private static final TypeToken<?> MAP_TYPE = new MapTypeToken();

	private static final TypeToken<?> LIST_TYPE = new ListTypeToken();

	private Gson gson = new GsonBuilder().create();

	@Override
	public Map<String, Object> parseMap(String json) {
		if (json != null) {
			json = json.trim();
			if (json.startsWith("{")) {
				return this.gson.fromJson(json, MAP_TYPE.getType());
			}
		}
		throw new IllegalArgumentException("Cannot parse JSON");
	}

	@Override
	public List<Object> parseList(String json) {
		if (json != null) {
			json = json.trim();
			if (json.startsWith("[")) {
				return this.gson.fromJson(json, LIST_TYPE.getType());
			}
		}
		throw new IllegalArgumentException("Cannot parse JSON");
	}

	private static final class MapTypeToken extends TypeToken<Map<String, Object>> {

	}

	private static final class ListTypeToken extends TypeToken<List<Object>> {

	}

}
