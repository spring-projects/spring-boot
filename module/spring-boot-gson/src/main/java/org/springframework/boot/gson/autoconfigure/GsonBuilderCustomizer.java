/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gson.autoconfigure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link GsonBuilder} to fine-tune its auto-configuration before it creates a
 * {@link Gson} instance.
 *
 * @author Ivan Golovko
 * @since 4.0.0
 */
@FunctionalInterface
public interface GsonBuilderCustomizer {

	/**
	 * Customize the GsonBuilder.
	 * @param gsonBuilder the GsonBuilder to customize
	 */
	void customize(GsonBuilder gsonBuilder);

}
