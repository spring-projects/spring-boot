/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import org.springframework.util.ClassUtils;

/**
 * Factory to create a {@link JsonParser}.
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see JacksonJsonParser
 * @see GsonJsonParser
 * @see BasicJsonParser
 */
public abstract class JsonParserFactory {

	/**
	 * Static factory for the "best" JSON parser available on the classpath. Tries
	 * Jackson, then Gson, and then falls back to the {@link BasicJsonParser}.
	 * @return a {@link JsonParser}
	 */
	public static JsonParser getJsonParser() {
		if (ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", null)) {
			return new JacksonJsonParser();
		}
		if (ClassUtils.isPresent("com.google.gson.Gson", null)) {
			return new GsonJsonParser();
		}
		return new BasicJsonParser();
	}

}
