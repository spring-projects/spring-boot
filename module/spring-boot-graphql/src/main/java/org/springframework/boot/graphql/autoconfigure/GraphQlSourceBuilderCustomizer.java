/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure;

import org.springframework.graphql.execution.GraphQlSource;

/**
 * Callback interface that can be implemented by beans wishing to customize properties of
 * {@link org.springframework.graphql.execution.GraphQlSource.SchemaResourceBuilder
 * Builder} to fine-tune its auto-configuration.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.0
 */
@FunctionalInterface
public interface GraphQlSourceBuilderCustomizer {

	/**
	 * Customize the
	 * {@link org.springframework.graphql.execution.GraphQlSource.SchemaResourceBuilder
	 * Builder} instance.
	 * @param builder builder the builder to customize
	 */
	void customize(GraphQlSource.SchemaResourceBuilder builder);

}
