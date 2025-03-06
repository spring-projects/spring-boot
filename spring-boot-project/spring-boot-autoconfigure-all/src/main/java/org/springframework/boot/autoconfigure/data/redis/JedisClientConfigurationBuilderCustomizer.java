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

import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link JedisClientConfiguration} through a {@link JedisClientConfigurationBuilder
 * JedisClientConfiguration.JedisClientConfigurationBuilder} whilst retaining default
 * auto-configuration.
 *
 * @author Mark Paluch
 * @since 2.0.0
 */
@FunctionalInterface
public interface JedisClientConfigurationBuilderCustomizer {

	/**
	 * Customize the {@link JedisClientConfigurationBuilder}.
	 * @param clientConfigurationBuilder the builder to customize
	 */
	void customize(JedisClientConfigurationBuilder clientConfigurationBuilder);

}
