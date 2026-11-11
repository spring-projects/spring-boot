/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

/**
 * Callback interface that can be implemented by beans wishing to customize the Artemis
 * JMS server {@link Configuration} to fine-tune its auto-configuration before it is used
 * by the auto-configured {@link EmbeddedActiveMQ} instance.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @since 4.0.0
 * @see ArtemisAutoConfiguration
 */
@FunctionalInterface
public interface ArtemisConfigurationCustomizer {

	/**
	 * Customize the configuration.
	 * @param configuration the configuration to customize
	 */
	void customize(Configuration configuration);

}
