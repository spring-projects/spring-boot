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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Redis support.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Christoph Strobl
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Marco Aust
 * @author Mark Paluch
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(RedisOperations.class)
@EnableConfigurationProperties(RedisProperties.class)
@Import({ LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class })
public class RedisAutoConfiguration {

	/**
	 * Generates a RedisConnectionDetails bean if no bean of type RedisConnectionDetails
	 * is present. Uses the RedisProperties to create a PropertiesRedisConnectionDetails
	 * object.
	 * @param properties the RedisProperties object used to create the
	 * PropertiesRedisConnectionDetails object
	 * @return a RedisConnectionDetails bean
	 */
	@Bean
	@ConditionalOnMissingBean(RedisConnectionDetails.class)
	PropertiesRedisConnectionDetails redisConnectionDetails(RedisProperties properties) {
		return new PropertiesRedisConnectionDetails(properties);
	}

	/**
	 * Creates a RedisTemplate bean if no bean with the name "redisTemplate" is already
	 * present in the application context. This method is conditionally executed only if a
	 * RedisConnectionFactory bean is present and there is no existing bean with the name
	 * "redisTemplate".
	 * @param redisConnectionFactory the RedisConnectionFactory bean used to create the
	 * RedisTemplate
	 * @return the RedisTemplate bean
	 */
	@Bean
	@ConditionalOnMissingBean(name = "redisTemplate")
	@ConditionalOnSingleCandidate(RedisConnectionFactory.class)
	public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	/**
	 * Creates a new instance of {@link StringRedisTemplate} if no other bean of the same
	 * type is present in the application context. This bean is conditionally created only
	 * if a single candidate bean of type {@link RedisConnectionFactory} is available.
	 * @param redisConnectionFactory the {@link RedisConnectionFactory} to be used for
	 * creating the {@link StringRedisTemplate}
	 * @return a new instance of {@link StringRedisTemplate} using the provided
	 * {@link RedisConnectionFactory}
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnSingleCandidate(RedisConnectionFactory.class)
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}

}
