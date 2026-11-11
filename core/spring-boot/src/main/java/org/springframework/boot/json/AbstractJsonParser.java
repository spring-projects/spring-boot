/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ReflectionUtils;

/**
 * Base class for parsers wrapped or implemented in this package.
 *
 * @author Anton Telechev
 * @author Phillip Webb
 * @since 2.0.1
 */
public abstract class AbstractJsonParser implements JsonParser {

	protected final Map<String, Object> parseMap(@Nullable String json, Function<String, Map<String, Object>> parser) {
		return trimParse(json, "{", parser);
	}

	protected final List<Object> parseList(@Nullable String json, Function<String, List<Object>> parser) {
		return trimParse(json, "[", parser);
	}

	protected final <T> T trimParse(@Nullable String json, String prefix, Function<String, T> parser) {
		String trimmed = (json != null) ? json.trim() : "";
		if (trimmed.startsWith(prefix)) {
			return parser.apply(trimmed);
		}
		throw new JsonParseException();
	}

	protected final <T> T tryParse(Callable<T> parser, Class<? extends Exception> check) {
		try {
			return parser.call();
		}
		catch (Exception ex) {
			if (check.isInstance(ex)) {
				throw new JsonParseException(ex);
			}
			ReflectionUtils.rethrowRuntimeException(ex);
			throw new IllegalStateException(ex);
		}
	}

}
