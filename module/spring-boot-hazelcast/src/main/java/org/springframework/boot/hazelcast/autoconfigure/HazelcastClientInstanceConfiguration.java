/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configuration for Hazelcast client instance.
 *
 * @author Dmytro Nosan
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(HazelcastConnectionDetails.class)
class HazelcastClientInstanceConfiguration {

	@Bean
	HazelcastInstance hazelcastInstance(HazelcastConnectionDetails hazelcastConnectionDetails) {
		ClientConfig config = hazelcastConnectionDetails.getClientConfig();
		return (!StringUtils.hasText(config.getInstanceName())) ? HazelcastClient.newHazelcastClient(config)
				: HazelcastClient.getOrCreateHazelcastClient(config);
	}

}
