/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.util.ReflectionUtils;

/**
 * Base class for parsers wrapped or implemented in this package.
 *
 * @author Anton Telechev
 * @author Phillip Webb
 * @since 2.0.1
 */
public abstract class AbstractJsonParser implements JsonParser {

	protected final Map<String, Object> parseMap(String json,
			Function<String, Map<String, Object>> parser) {
		return trimParse(json, "{", parser);
	}

	protected final List<Object> parseList(String json,
			Function<String, List<Object>> parser) {
		return trimParse(json, "[", parser);
	}

	protected final <T> T trimParse(String json, String prefix,
			Function<String, T> parser) {
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
			if (check.isAssignableFrom(ex.getClass())) {
				throw new JsonParseException(ex);
			}
			ReflectionUtils.rethrowRuntimeException(ex);
			throw new IllegalStateException(ex);
		}
	}

}
