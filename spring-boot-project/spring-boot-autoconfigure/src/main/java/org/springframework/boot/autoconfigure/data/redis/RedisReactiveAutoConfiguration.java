/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.redis;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive Redis
 * support.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveRedisConnectionFactory.class, ReactiveRedisTemplate.class,
		Flux.class })
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "reactiveRedisTemplate")
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	public ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate(
			ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
			ResourceLoader resourceLoader) {
		JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer(
				resourceLoader.getClassLoader());
		RedisSerializationContext<Object, Object> serializationContext = RedisSerializationContext
				.newSerializationContext().key(jdkSerializer).value(jdkSerializer)
				.hashKey(jdkSerializer).hashValue(jdkSerializer).build();
		return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory,
				serializationContext);
	}

}
