/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import org.cache2k.Cache2kBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the default
 * setup for caches added to the manager through addCaches and for dynamically created
 * caches.
 *
 * @author Jens Wilke
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public interface Cache2kBuilderCustomizer {

	/**
	 * Customize the default cache settings.
	 * @param builder the builder to customize
	 */
	void customize(Cache2kBuilder<?, ?> builder);

}
