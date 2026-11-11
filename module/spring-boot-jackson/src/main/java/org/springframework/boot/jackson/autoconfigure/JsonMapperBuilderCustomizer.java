/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.databind.json.JsonMapper;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link JsonMapper} through {@link tools.jackson.databind.json.JsonMapper.Builder} to
 * fine-tune its auto-configuration.
 *
 * @author Grzegorz Poznachowski
 * @since 4.0.0
 */
@FunctionalInterface
public interface JsonMapperBuilderCustomizer {

	/**
	 * Customize the JsonMapper.Builder.
	 * @param jsonMapperBuilder the builder to customize
	 */
	void customize(JsonMapper.Builder jsonMapperBuilder);

}
