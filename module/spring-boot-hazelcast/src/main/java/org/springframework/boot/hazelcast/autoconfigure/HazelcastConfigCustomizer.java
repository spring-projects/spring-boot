/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.config.Config;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * auto-configured Hazelcast server {@link Config configuration}.
 *
 * @author Jaromir Hamala
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface HazelcastConfigCustomizer {

	/**
	 * Customize the configuration.
	 * @param config the {@link Config} to customize
	 */
	void customize(Config config);

}
