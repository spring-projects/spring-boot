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

package org.springframework.boot.jackson;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.jackson.JsonComponent.Scope;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Spring Bean and Jackson {@link Module} to register {@link JsonComponent @JsonComponent}
 * annotated beans.
 *
 * @author Phillip Webb
 * @author Paul Aly
 * @since 1.4.0
 * @see JsonComponent
 */
public class JsonComponentModule extends SimpleModule implements BeanFactoryAware, InitializingBean {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		registerJsonComponents();
	}

	public void registerJsonComponents() {
		BeanFactory beanFactory = this.beanFactory;
		while (beanFactory != null) {
			if (beanFactory instanceof ListableBeanFactory) {
				addJsonBeans((ListableBeanFactory) beanFactory);
			}
			beanFactory = (beanFactory instanceof HierarchicalBeanFactory)
					? ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory() : null;
		}
	}

	private void addJsonBeans(ListableBeanFactory beanFactory) {
		Map<String, Object> beans = beanFactory.getBeansWithAnnotation(JsonComponent.class);
		for (Object bean : beans.values()) {
			addJsonBean(bean);
		}
	}

	private void addJsonBean(Object bean) {
		MergedAnnotation<JsonComponent> annotation = MergedAnnotations
			.from(bean.getClass(), SearchStrategy.TYPE_HIERARCHY)
			.get(JsonComponent.class);
		Class<?>[] types = annotation.getClassArray("type");
		Scope scope = annotation.getEnum("scope", JsonComponent.Scope.class);
		addJsonBean(bean, types, scope);
	}

	private void addJsonBean(Object bean, Class<?>[] types, Scope scope) {
		if (bean instanceof JsonSerializer) {
			addJsonSerializerBean((JsonSerializer<?>) bean, scope, types);
		}
		else if (bean instanceof JsonDeserializer) {
			addJsonDeserializerBean((JsonDeserializer<?>) bean, types);
		}
		else if (bean instanceof KeyDeserializer) {
			addKeyDeserializerBean((KeyDeserializer) bean, types);
		}
		for (Class<?> innerClass : bean.getClass().getDeclaredClasses()) {
			if (isSuitableInnerClass(innerClass)) {
				Object innerInstance = BeanUtils.instantiateClass(innerClass);
				addJsonBean(innerInstance, types, scope);
			}
		}
	}

	private boolean isSuitableInnerClass(Class<?> innerClass) {
		return !Modifier.isAbstract(innerClass.getModifiers()) && (JsonSerializer.class.isAssignableFrom(innerClass)
				|| JsonDeserializer.class.isAssignableFrom(innerClass)
				|| KeyDeserializer.class.isAssignableFrom(innerClass));
	}

	@SuppressWarnings("unchecked")
	private <T> void addJsonSerializerBean(JsonSerializer<T> serializer, JsonComponent.Scope scope, Class<?>[] types) {
		Class<T> baseType = (Class<T>) ResolvableType.forClass(JsonSerializer.class, serializer.getClass())
			.resolveGeneric();
		addBeanToModule(serializer, baseType, types,
				(scope == Scope.VALUES) ? this::addSerializer : this::addKeySerializer);

	}

	@SuppressWarnings("unchecked")
	private <T> void addJsonDeserializerBean(JsonDeserializer<T> deserializer, Class<?>[] types) {
		Class<T> baseType = (Class<T>) ResolvableType.forClass(JsonDeserializer.class, deserializer.getClass())
			.resolveGeneric();
		addBeanToModule(deserializer, baseType, types, this::addDeserializer);
	}

	private void addKeyDeserializerBean(KeyDeserializer deserializer, Class<?>[] types) {
		Assert.notEmpty(types, "Type must be specified for KeyDeserializer");
		addBeanToModule(deserializer, Object.class, types, this::addKeyDeserializer);
	}

	@SuppressWarnings("unchecked")
	private <E, T> void addBeanToModule(E element, Class<T> baseType, Class<?>[] types,
			BiConsumer<Class<T>, E> consumer) {
		if (ObjectUtils.isEmpty(types)) {
			consumer.accept(baseType, element);
			return;
		}
		for (Class<?> type : types) {
			Assert.isAssignable(baseType, type);
			consumer.accept((Class<T>) type, element);
		}
	}

}
