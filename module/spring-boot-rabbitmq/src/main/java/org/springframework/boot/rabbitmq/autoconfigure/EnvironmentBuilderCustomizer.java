/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.EnvironmentBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link EnvironmentBuilder} to fine-tune its auto-configuration before it creates an
 * {@link Environment}.
 *
 * @author Andy Wilkinson
 * @since 4.2.0
 */
@FunctionalInterface
public interface EnvironmentBuilderCustomizer {

	/**
	 * Customize the {@code EnvironmentBuilder}.
	 * @param builder the builder to customize
	 */
	void customize(EnvironmentBuilder builder);

}
