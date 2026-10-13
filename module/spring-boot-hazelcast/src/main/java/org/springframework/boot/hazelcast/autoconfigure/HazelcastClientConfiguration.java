/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for Hazelcast client.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HazelcastClient.class)
@ConditionalOnMissingBean(HazelcastInstance.class)
@Import({ HazelcastConnectionDetailsConfiguration.class, HazelcastClientInstanceConfiguration.class })
class HazelcastClientConfiguration {

	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.client.config";

}
