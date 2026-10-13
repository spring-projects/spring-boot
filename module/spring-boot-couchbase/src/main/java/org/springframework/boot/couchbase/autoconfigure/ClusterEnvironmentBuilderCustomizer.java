/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure;

import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.env.ClusterEnvironment.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder ClusterEnvironment.Builder} to fine-tune its auto-configuration before
 * it creates the {@link ClusterEnvironment}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface ClusterEnvironmentBuilderCustomizer {

	/**
	 * Customize the {@link Builder ClusterEnvironment.Builder}.
	 * @param builder the builder to customize
	 */
	void customize(ClusterEnvironment.Builder builder);

}
