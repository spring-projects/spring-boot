/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.redis.receiving.custom;

import org.springframework.boot.data.redis.autoconfigure.RedisMessageListenerContainerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration(proxyBeanMethods = false)
public class MyRedisConfiguration {

	@Bean
	public RedisMessageListenerContainer myRedisMessageListenerContainer(
			RedisMessageListenerContainerConfigurer configurer, RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		configurer.configure(container, connectionFactory);
		// ... custom configuration
		return container;
	}

}
