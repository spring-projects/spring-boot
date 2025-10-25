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

package org.springframework.boot.jackson;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.jackson.JacksonComponent.Scope;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link JacksonModule} to register {@link JacksonComponent @JacksonComponent} annotated
 * beans.
 *
 * @author Phillip Webb
 * @author Paul Aly
 * @since 4.0.0
 * @see JacksonComponent
 */
public class JacksonComponentModule extends SimpleModule implements BeanFactoryAware, InitializingBean {

	@SuppressWarnings("NullAway.Init")
	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		registerJacksonComponents();
	}

	public void registerJacksonComponents() {
		BeanFactory beanFactory = this.beanFactory;
		while (beanFactory != null) {
			if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
				addJacksonComponentBeans(listableBeanFactory);
			}
			beanFactory = (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory)
					? hierarchicalBeanFactory.getParentBeanFactory() : null;
		}
	}

	private void addJacksonComponentBeans(ListableBeanFactory beanFactory) {
		Map<String, Object> beans = beanFactory.getBeansWithAnnotation(JacksonComponent.class);
		for (Object bean : beans.values()) {
			addJacksonComponentBean(bean);
		}
	}

	private void addJacksonComponentBean(Object bean) {
		MergedAnnotation<JacksonComponent> annotation = MergedAnnotations
			.from(bean.getClass(), SearchStrategy.TYPE_HIERARCHY)
			.get(JacksonComponent.class);
		Class<?>[] types = annotation.getClassArray("type");
		Scope scope = annotation.getEnum("scope", JacksonComponent.Scope.class);
		addJacksonComponentBean(bean, types, scope);
	}

	private void addJacksonComponentBean(Object bean, Class<?>[] types, Scope scope) {
		if (bean instanceof ValueSerializer<?> jsonSerializer) {
			addValueSerializerBean(jsonSerializer, scope, types);
		}
		else if (bean instanceof ValueDeserializer<?> jsonDeserializer) {
			addValueDeserializerBean(jsonDeserializer, types);
		}
		else if (bean instanceof KeyDeserializer keyDeserializer) {
			addKeyDeserializerBean(keyDeserializer, types);
		}
		for (Class<?> innerClass : bean.getClass().getDeclaredClasses()) {
			if (isSuitableInnerClass(innerClass)) {
				Object innerInstance = BeanUtils.instantiateClass(innerClass);
				addJacksonComponentBean(innerInstance, types, scope);
			}
		}
	}

	private static boolean isSuitableInnerClass(Class<?> innerClass) {
		return !Modifier.isAbstract(innerClass.getModifiers()) && (ValueSerializer.class.isAssignableFrom(innerClass)
				|| ValueDeserializer.class.isAssignableFrom(innerClass)
				|| KeyDeserializer.class.isAssignableFrom(innerClass));
	}

	@SuppressWarnings("unchecked")
	private <T> void addValueSerializerBean(ValueSerializer<T> serializer, JacksonComponent.Scope scope,
			Class<?>[] types) {
		Class<T> baseType = (Class<T>) ResolvableType.forClass(ValueSerializer.class, serializer.getClass())
			.resolveGeneric();
		Assert.state(baseType != null, "'baseType' must not be null");
		addBeanToModule(serializer, baseType, types,
				(scope == Scope.VALUES) ? this::addSerializer : this::addKeySerializer);

	}

	@SuppressWarnings("unchecked")
	private <T> void addValueDeserializerBean(ValueDeserializer<T> deserializer, Class<?>[] types) {
		Class<T> baseType = (Class<T>) ResolvableType.forClass(ValueDeserializer.class, deserializer.getClass())
			.resolveGeneric();
		Assert.state(baseType != null, "'baseType' must not be null");
		addBeanToModule(deserializer, baseType, types, this::addDeserializer);
	}

	private void addKeyDeserializerBean(KeyDeserializer deserializer, Class<?>[] types) {
		Assert.notEmpty(types, "'types' must not be empty");
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

	static class JacksonComponentBeanFactoryInitializationAotProcessor
			implements BeanFactoryInitializationAotProcessor {

		@Override
		public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {
			String[] jacksonComponents = beanFactory.getBeanNamesForAnnotation(JacksonComponent.class);
			Map<Class<?>, List<Class<?>>> innerComponents = new HashMap<>();
			for (String jacksonComponent : jacksonComponents) {
				Class<?> type = beanFactory.getType(jacksonComponent, true);
				Assert.state(type != null, "'type' must not be null");
				for (Class<?> declaredClass : type.getDeclaredClasses()) {
					if (isSuitableInnerClass(declaredClass)) {
						innerComponents.computeIfAbsent(type, (t) -> new ArrayList<>()).add(declaredClass);
					}
				}
			}
			return innerComponents.isEmpty() ? null : new JacksonComponentAotContribution(innerComponents);
		}

	}

	private static final class JacksonComponentAotContribution implements BeanFactoryInitializationAotContribution {

		private final Map<Class<?>, List<Class<?>>> innerComponents;

		private JacksonComponentAotContribution(Map<Class<?>, List<Class<?>>> innerComponents) {
			this.innerComponents = innerComponents;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
			this.innerComponents.forEach((outer, inners) -> {
				reflection.registerType(outer);
				inners.forEach((inner) -> reflection.registerType(inner, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
			});
		}

	}

}
