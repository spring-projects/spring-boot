/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.client.config.ClientConfig;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a client connection to a Hazelcast instance.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
public interface HazelcastConnectionDetails extends ConnectionDetails {

	/**
	 * The {@link ClientConfig} for Hazelcast client.
	 * @return the client config
	 */
	ClientConfig getClientConfig();

}
