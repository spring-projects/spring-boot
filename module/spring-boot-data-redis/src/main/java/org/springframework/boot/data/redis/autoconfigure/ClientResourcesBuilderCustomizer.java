/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.ClientResources.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder} to fine-tune its auto-configuration before it creates the
 * {@link ClientResources}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public interface ClientResourcesBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param clientResourcesBuilder the builder to customize
	 */
	void customize(Builder clientResourcesBuilder);

}
