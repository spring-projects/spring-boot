/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.autoconfigure;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link CqlSessionBuilder} to fine-tune its auto-configuration before it creates a
 * {@link CqlSession}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface CqlSessionBuilderCustomizer {

	/**
	 * Customize the {@link CqlSessionBuilder}.
	 * @param cqlSessionBuilder the builder to customize
	 */
	void customize(CqlSessionBuilder cqlSessionBuilder);

}
