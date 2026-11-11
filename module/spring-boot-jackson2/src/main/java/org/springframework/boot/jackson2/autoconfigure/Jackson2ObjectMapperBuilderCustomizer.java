/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link ObjectMapper} through
 * {@link org.springframework.http.converter.json.Jackson2ObjectMapperBuilder} to
 * fine-tune its auto-configuration.
 *
 * @author Grzegorz Poznachowski
 * @since 4.0.0
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3.
 */
@FunctionalInterface
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public interface Jackson2ObjectMapperBuilderCustomizer {

	/**
	 * Customize the JacksonObjectMapperBuilder.
	 * @param jacksonObjectMapperBuilder the JacksonObjectMapperBuilder to customize
	 */
	void customize(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder);

}
