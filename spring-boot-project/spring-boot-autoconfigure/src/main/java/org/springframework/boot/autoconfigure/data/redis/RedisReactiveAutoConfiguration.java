/*
 * Copyright 2012-2023 the original author or authors.
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
 * @since 2.0.0
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass({ ReactiveRedisConnectionFactory.class, ReactiveRedisTemplate.class, Flux.class })
public class RedisReactiveAutoConfiguration {

	/**
     * Creates a {@link ReactiveRedisTemplate} bean if there is no existing bean with the name "reactiveRedisTemplate"
     * and if a bean of type {@link ReactiveRedisConnectionFactory} is present.
     * 
     * The {@link ReactiveRedisTemplate} is configured with a {@link RedisSerializer} using the provided
     * {@link ResourceLoader} to obtain the class loader. The serialization context is then built using the
     * {@link RedisSerializationContext} with the same {@link RedisSerializer} for keys, values, hash keys, and hash values.
     * 
     * @param reactiveRedisConnectionFactory the {@link ReactiveRedisConnectionFactory} bean
     * @param resourceLoader the {@link ResourceLoader} used to obtain the class loader
     * @return the created {@link ReactiveRedisTemplate} bean
     */
    @Bean
	@ConditionalOnMissingBean(name = "reactiveRedisTemplate")
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	public ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate(
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

	/**
     * Creates a new instance of ReactiveStringRedisTemplate if no bean with the name "reactiveStringRedisTemplate" is already present in the application context and if a bean of type ReactiveRedisConnectionFactory is present.
     * 
     * @param reactiveRedisConnectionFactory the ReactiveRedisConnectionFactory used to create the ReactiveStringRedisTemplate
     * @return the newly created ReactiveStringRedisTemplate
     */
    @Bean
	@ConditionalOnMissingBean(name = "reactiveStringRedisTemplate")
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
			ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
		return new ReactiveStringRedisTemplate(reactiveRedisConnectionFactory);
	}

}
