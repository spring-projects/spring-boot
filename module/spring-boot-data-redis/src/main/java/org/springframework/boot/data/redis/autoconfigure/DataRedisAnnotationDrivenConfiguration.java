/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.data.redis.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.annotation.EnableRedisListeners;
import org.springframework.data.redis.config.RedisListenerConfigUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.Message;

/**
 * Configuration for Redis annotation-driven listeners.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ EnableRedisListeners.class, Message.class })
class DataRedisAnnotationDrivenConfiguration {

	private static final String DEFAULT_MESSAGE_LISTENER_BEAN_NAME = RedisListenerConfigUtils.REDIS_MESSAGE_LISTENER_BEAN_NAME;

	private final DataRedisProperties properties;

	DataRedisAnnotationDrivenConfiguration(DataRedisProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	RedisMessageListenerContainerConfigurer redisMessageListenerContainerConfigurer() {
		return new RedisMessageListenerContainerConfigurer(this.properties);
	}

	@Bean(name = DEFAULT_MESSAGE_LISTENER_BEAN_NAME)
	@ConditionalOnSingleCandidate(RedisConnectionFactory.class)
	@ConditionalOnMissingBean(name = DEFAULT_MESSAGE_LISTENER_BEAN_NAME)
	RedisMessageListenerContainer redisMessageListenerContainer(RedisMessageListenerContainerConfigurer configurer,
			RedisConnectionFactory redisConnectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		configurer.configure(container, redisConnectionFactory);
		return container;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableRedisListeners
	@ConditionalOnMissingBean(name = RedisListenerConfigUtils.REDIS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableRedisListenersConfiguration {

	}

}
