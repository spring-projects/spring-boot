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

package org.springframework.boot.jackson.autoconfigure;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.util.Converter;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * Package-private adaptation of Spring Framework's
 * {@link org.springframework.http.support.JacksonHandlerInstantiator}. Allows Spring Boot
 * to offer bean-based handler instantiation without requiring {@code spring-web}.
 *
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 * @see org.springframework.http.support.JacksonHandlerInstantiator
 */
class SpringBeanHandlerInstantiator extends HandlerInstantiator {

	private final AutowireCapableBeanFactory beanFactory;

	SpringBeanHandlerInstantiator(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	@Nullable public ValueDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated,
			Class<?> deserClass) {
		return (ValueDeserializer<?>) this.beanFactory.createBean(deserClass);
	}

	@Override
	public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated,
			Class<?> keyDeserClass) {
		return (KeyDeserializer) this.beanFactory.createBean(keyDeserClass);
	}

	@Override
	public ValueSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
		return (ValueSerializer<?>) this.beanFactory.createBean(serClass);
	}

	@Override
	public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated,
			Class<?> builderClass) {
		return (TypeResolverBuilder<?>) this.beanFactory.createBean(builderClass);
	}

	@Override
	public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
		return (TypeIdResolver) this.beanFactory.createBean(resolverClass);
	}

	@Override
	public ValueInstantiator valueInstantiatorInstance(MapperConfig<?> config, Annotated annotated,
			Class<?> implClass) {
		return (ValueInstantiator) this.beanFactory.createBean(implClass);
	}

	@Override
	public ObjectIdGenerator<?> objectIdGeneratorInstance(MapperConfig<?> config, Annotated annotated,
			Class<?> implClass) {
		return (ObjectIdGenerator<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public ObjectIdResolver resolverIdGeneratorInstance(MapperConfig<?> config, Annotated annotated,
			Class<?> implClass) {
		return (ObjectIdResolver) this.beanFactory.createBean(implClass);
	}

	@Override
	public PropertyNamingStrategy namingStrategyInstance(MapperConfig<?> config, Annotated annotated,
			Class<?> implClass) {
		return (PropertyNamingStrategy) this.beanFactory.createBean(implClass);
	}

	@Override
	public Converter<?, ?> converterInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
		return (Converter<?, ?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public VirtualBeanPropertyWriter virtualPropertyWriterInstance(MapperConfig<?> config, Class<?> implClass) {
		return (VirtualBeanPropertyWriter) this.beanFactory.createBean(implClass);
	}

}
