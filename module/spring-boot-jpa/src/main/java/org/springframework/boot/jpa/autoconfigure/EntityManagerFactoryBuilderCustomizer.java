/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jpa.autoconfigure;

import org.springframework.boot.jpa.EntityManagerFactoryBuilder;

/**
 * Callback interface that can be used to customize the
 * {@link EntityManagerFactoryBuilder} to fine-tune its auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface EntityManagerFactoryBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder to customize
	 */
	void customize(EntityManagerFactoryBuilder builder);

}
