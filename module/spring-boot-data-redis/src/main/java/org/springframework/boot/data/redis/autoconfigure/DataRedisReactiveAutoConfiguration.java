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

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive Redis
 * support.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = DataRedisAutoConfiguration.class)
@ConditionalOnClass({ ReactiveRedisConnectionFactory.class, ReactiveRedisTemplate.class, Flux.class })
public final class DataRedisReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "reactiveRedisTemplate")
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate(
			ReactiveRedisConnectionFactory reactiveRedisConnectionFactory, ResourceLoader resourceLoader) {
		RedisSerializer<Object> javaSerializer = RedisSerializer.java(resourceLoader.getClassLoader());
		RedisSerializationContext<Object, Object> serializationContext = RedisSerializationContext
			.newSerializationContext()
			.key(javaSerializer)
			.value(javaSerializer)
			.hashKey(javaSerializer)
			.hashValue(javaSerializer)
			.build();
		return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, serializationContext);
	}

	@Bean
	@ConditionalOnMissingBean(name = "reactiveStringRedisTemplate")
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	ReactiveStringRedisTemplate reactiveStringRedisTemplate(
			ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
		return new ReactiveStringRedisTemplate(reactiveRedisConnectionFactory);
	}

}
