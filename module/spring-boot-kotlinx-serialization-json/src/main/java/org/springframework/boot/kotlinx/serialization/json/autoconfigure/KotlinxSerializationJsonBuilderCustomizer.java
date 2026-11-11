/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kotlinx.serialization.json.autoconfigure;

import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link Json} through {@link JsonBuilder} retaining its default configuration.
 *
 * @author Dmitry Sulman
 * @since 4.0.0
 */
@FunctionalInterface
public interface KotlinxSerializationJsonBuilderCustomizer {

	/**
	 * Customize the Kotlin Serialization {@link Json} through {@link JsonBuilder}.
	 * @param jsonBuilder the {@link JsonBuilder} to customize
	 */
	void customize(JsonBuilder jsonBuilder);

}
