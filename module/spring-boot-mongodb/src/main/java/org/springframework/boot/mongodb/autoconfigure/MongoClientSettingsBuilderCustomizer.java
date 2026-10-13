/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import com.mongodb.MongoClientSettings.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder MongoClientSettings.Builder} to fine-tune its auto-configuration. before
 * it creates a {@link com.mongodb.MongoClientSettings}.
 *
 * @author Mark Paluch
 * @since 4.0.0
 */
@FunctionalInterface
public interface MongoClientSettingsBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param clientSettingsBuilder the builder to customize
	 */
	void customize(Builder clientSettingsBuilder);

}
