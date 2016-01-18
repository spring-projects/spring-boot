/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.ObjectUtils;

/**
 * Configures a {@link RedisTemplate} with a serializer for keys, values, hash keys, and
 * hash values that is compatible with the split classloader used for restarts.
 *
 * @author Andy Wilkinson
 * @author Rob Winch
 * @see RedisTemplate#setHashKeySerializer(RedisSerializer)
 * @see RedisTemplate#setHashValueSerializer(RedisSerializer)
 * @see RedisTemplate#setKeySerializer(RedisSerializer)
 * @see RedisTemplate#setValueSerializer(RedisSerializer)
 */
class RestartCompatibleRedisSerializerConfigurer implements BeanClassLoaderAware {

	private final RedisTemplate<?, ?> redisTemplate;

	private volatile ClassLoader classLoader;

	RestartCompatibleRedisSerializerConfigurer(RedisTemplate<?, ?> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@PostConstruct
	void configureTemplateSerializers() {
		RestartCompatibleRedisSerializer serializer = new RestartCompatibleRedisSerializer(
				this.classLoader);
		this.redisTemplate.setHashKeySerializer(serializer);
		this.redisTemplate.setHashValueSerializer(serializer);
		this.redisTemplate.setKeySerializer(serializer);
		this.redisTemplate.setValueSerializer(serializer);
	}

	static class RestartCompatibleRedisSerializer implements RedisSerializer<Object> {

		private static final byte[] NO_BYTES = new byte[0];

		private final Converter<Object, byte[]> serializer = new SerializingConverter();

		private final Converter<byte[], Object> deserializer;

		RestartCompatibleRedisSerializer(ClassLoader classLoader) {
			this.deserializer = new DeserializingConverter(
					new DefaultDeserializer(classLoader));
		}

		@Override
		public Object deserialize(byte[] bytes) {
			try {
				return (ObjectUtils.isEmpty(bytes) ? null
						: this.deserializer.convert(bytes));
			}
			catch (Exception ex) {
				throw new SerializationException("Cannot deserialize", ex);
			}
		}

		@Override
		public byte[] serialize(Object object) {
			try {
				return (object == null ? NO_BYTES : this.serializer.convert(object));
			}
			catch (Exception ex) {
				throw new SerializationException("Cannot serialize", ex);
			}
		}

	}

}
