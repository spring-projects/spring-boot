/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hazelcast IMDG. Creates a
 * {@link HazelcastInstance} based on explicit configuration or when a default
 * configuration file is found in the environment.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0.0
 * @see HazelcastConfigResourceCondition
 */
@AutoConfiguration
@ConditionalOnClass(HazelcastInstance.class)
@EnableConfigurationProperties(HazelcastProperties.class)
@Import({ HazelcastClientConfiguration.class, HazelcastServerConfiguration.class })
public final class HazelcastAutoConfiguration {

}
