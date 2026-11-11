/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jspecify.annotations.Nullable;

/**
 * Thin wrapper to adapt {@link Gson} to a {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @since 1.2.0
 * @see JsonParserFactory
 */
public class GsonJsonParser extends AbstractJsonParser {

	private static final TypeToken<?> MAP_TYPE = new MapTypeToken();

	private static final TypeToken<?> LIST_TYPE = new ListTypeToken();

	private final Gson gson = new GsonBuilder().create();

	@Override
	public Map<String, Object> parseMap(@Nullable String json) {
		return tryParse(() -> parseMap(json, (trimmed) -> this.gson.fromJson(trimmed, MAP_TYPE.getType())),
				Exception.class);
	}

	@Override
	public List<Object> parseList(@Nullable String json) {
		return tryParse(() -> parseList(json, (trimmed) -> this.gson.fromJson(trimmed, LIST_TYPE.getType())),
				Exception.class);
	}

	private static final class MapTypeToken extends TypeToken<Map<String, Object>> {

	}

	private static final class ListTypeToken extends TypeToken<List<Object>> {

	}

}
