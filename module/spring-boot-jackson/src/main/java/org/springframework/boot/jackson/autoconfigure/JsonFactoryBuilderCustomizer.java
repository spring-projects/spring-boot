/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link JsonFactory} through {@link JsonFactoryBuilder} to fine-tune its
 * auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@FunctionalInterface
public interface JsonFactoryBuilderCustomizer {

	/**
	 * Customize the JsonFactoryBuilder.
	 * @param jsonFactoryBuilder the builder to customize
	 */
	void customize(JsonFactoryBuilder jsonFactoryBuilder);

}
