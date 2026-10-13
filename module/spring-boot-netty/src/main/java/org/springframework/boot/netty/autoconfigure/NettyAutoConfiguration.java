/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.netty.autoconfigure;

import io.netty.util.NettyRuntime;
import io.netty.util.ResourceLeakDetector;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Netty.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(NettyRuntime.class)
@EnableConfigurationProperties(NettyProperties.class)
public final class NettyAutoConfiguration {

	NettyAutoConfiguration(NettyProperties properties) {
		if (properties.getLeakDetection() != null) {
			NettyProperties.LeakDetection leakDetection = properties.getLeakDetection();
			ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakDetection.name()));
		}
	}

	@Bean
	static LazyInitializationExcludeFilter nettyAutoConfigurationLazyInitializationExcludeFilter() {
		return LazyInitializationExcludeFilter.forBeanTypes(NettyAutoConfiguration.class);
	}

}
