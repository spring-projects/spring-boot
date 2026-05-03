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

import java.time.Duration;
import java.util.function.Predicate;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties.Listener;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties.Recovery;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.backoff.BackOff;

/**
 * Configure {@link RedisMessageListenerContainer} with sensible defaults tuned using
 * configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RedisMessageListenerContainer} whose configuration is based upon that produced
 * by auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
public class RedisMessageListenerContainerConfigurer {

	private final DataRedisProperties properties;

	public RedisMessageListenerContainerConfigurer(DataRedisProperties properties) {
		this.properties = properties;
	}

	/**
	 * Configure the specified Redis message listener container. The container can be
	 * further tuned and default settings can be overridden.
	 * @param container the {@link RedisMessageListenerContainer} instance to configure
	 * @param connectionFactory the {@link RedisConnectionFactory} to use
	 */
	public void configure(RedisMessageListenerContainer container, RedisConnectionFactory connectionFactory) {
		container.setConnectionFactory(connectionFactory);
		PropertyMapper map = PropertyMapper.get();
		Listener listenerProperties = this.properties.getListener();
		map.from(listenerProperties::isAutoStartup).to(container::setAutoStartup);
		map.from(listenerProperties::getSubscriptionRegistrationTimeout)
			.as(Duration::toMillis)
			.to(container::setMaxSubscriptionRegistrationWaitingTime);
		map.from(getRecoveryBackOff(listenerProperties.getRecovery())).to(container::setRecoveryBackoff);
	}

	static BackOff getRecoveryBackOff(Recovery recovery) {
		PropertyMapper map = PropertyMapper.get();
		RetryPolicy.Builder builder = RetryPolicy.builder().maxRetries(recovery.getMaxRetries());
		map.from(recovery.getDelay()).to(builder::delay);
		map.from(recovery.getMaxDelay()).when(Predicate.not(Duration::isZero)).to(builder::maxDelay);
		map.from(recovery.getMultiplier()).to(builder::multiplier);
		map.from(recovery.getJitter()).when((Predicate.not(Duration::isZero))).to(builder::jitter);
		return builder.build().getBackOff();
	}

}
