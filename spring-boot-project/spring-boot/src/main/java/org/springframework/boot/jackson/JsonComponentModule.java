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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

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

	/**
	 * Sets the bean factory for this JsonComponentModule.
	 * @param beanFactory the bean factory to be set
	 * @throws BeansException if an error occurs while setting the bean factory
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * This method is called after all the properties have been set for the
	 * JsonComponentModule class. It registers the JSON components.
	 */
	@Override
	public void afterPropertiesSet() {
		registerJsonComponents();
	}

	/**
	 * Registers JSON components.
	 *
	 * This method iterates through the bean factories starting from the current bean
	 * factory and its parent bean factories. If a bean factory is an instance of
	 * ListableBeanFactory, it adds JSON beans from that bean factory.
	 * @param None
	 * @return None
	 */
	public void registerJsonComponents() {
		BeanFactory beanFactory = this.beanFactory;
		while (beanFactory != null) {
			if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
				addJsonBeans(listableBeanFactory);
			}
			beanFactory = (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory)
					? hierarchicalBeanFactory.getParentBeanFactory() : null;
		}
	}

	/**
	 * Adds JSON beans to the module.
	 * @param beanFactory the bean factory to retrieve the beans from
	 */
	private void addJsonBeans(ListableBeanFactory beanFactory) {
		Map<String, Object> beans = beanFactory.getBeansWithAnnotation(JsonComponent.class);
		for (Object bean : beans.values()) {
			addJsonBean(bean);
		}
	}

	/**
	 * Adds a JSON bean to the module.
	 * @param bean the JSON bean to be added
	 */
	private void addJsonBean(Object bean) {
		MergedAnnotation<JsonComponent> annotation = MergedAnnotations
			.from(bean.getClass(), SearchStrategy.TYPE_HIERARCHY)
			.get(JsonComponent.class);
		Class<?>[] types = annotation.getClassArray("type");
		Scope scope = annotation.getEnum("scope", JsonComponent.Scope.class);
		addJsonBean(bean, types, scope);
	}

	/**
	 * Adds a JSON bean to the module.
	 * @param bean The JSON bean to be added.
	 * @param types The types associated with the bean.
	 * @param scope The scope of the bean.
	 */
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

	/**
	 * Checks if the given inner class is suitable for use as a JSON component.
	 * @param innerClass the inner class to check
	 * @return true if the inner class is suitable, false otherwise
	 */
	private static boolean isSuitableInnerClass(Class<?> innerClass) {
		return !Modifier.isAbstract(innerClass.getModifiers()) && (JsonSerializer.class.isAssignableFrom(innerClass)
				|| JsonDeserializer.class.isAssignableFrom(innerClass)
				|| KeyDeserializer.class.isAssignableFrom(innerClass));
	}

	/**
	 * Adds a JSON serializer bean to the module.
	 * @param serializer the JSON serializer to be added
	 * @param scope the scope of the serializer (VALUES or KEYS)
	 * @param types the types to be serialized by the serializer
	 * @param <T> the type of the serializer
	 */
	@SuppressWarnings("unchecked")
	private <T> void addJsonSerializerBean(JsonSerializer<T> serializer, JsonComponent.Scope scope, Class<?>[] types) {
		Class<T> baseType = (Class<T>) ResolvableType.forClass(JsonSerializer.class, serializer.getClass())
			.resolveGeneric();
		addBeanToModule(serializer, baseType, types,
				(scope == Scope.VALUES) ? this::addSerializer : this::addKeySerializer);

	}

	/**
	 * Adds a JSON deserializer bean to the module.
	 * @param deserializer the JSON deserializer to be added
	 * @param types the types to which the deserializer should be applied
	 * @param <T> the type of the deserializer
	 */
	@SuppressWarnings("unchecked")
	private <T> void addJsonDeserializerBean(JsonDeserializer<T> deserializer, Class<?>[] types) {
		Class<T> baseType = (Class<T>) ResolvableType.forClass(JsonDeserializer.class, deserializer.getClass())
			.resolveGeneric();
		addBeanToModule(deserializer, baseType, types, this::addDeserializer);
	}

	/**
	 * Adds a KeyDeserializer bean to the module.
	 * @param deserializer the KeyDeserializer to be added
	 * @param types the types for which the KeyDeserializer should be used
	 * @throws IllegalArgumentException if the types array is empty or null
	 */
	private void addKeyDeserializerBean(KeyDeserializer deserializer, Class<?>[] types) {
		Assert.notEmpty(types, "Type must be specified for KeyDeserializer");
		addBeanToModule(deserializer, Object.class, types, this::addKeyDeserializer);
	}

	/**
	 * Adds a bean to the module.
	 * @param element the element to be added
	 * @param baseType the base type of the bean
	 * @param types the types of the bean
	 * @param consumer the consumer function to accept the bean
	 * @param <E> the type of the element
	 * @param <T> the base type of the bean
	 */
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

	/**
	 * JsonComponentBeanFactoryInitializationAotProcessor class.
	 */
	static class JsonComponentBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

		/**
		 * Processes the bean factory ahead of time to find all classes annotated
		 * with @JsonComponent and their suitable inner classes.
		 * @param beanFactory the configurable listable bean factory
		 * @return the bean factory initialization AOT contribution containing the inner
		 * components of the classes annotated with @JsonComponent, or null if no inner
		 * components are found
		 */
		@Override
		public BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {
			String[] jsonComponents = beanFactory.getBeanNamesForAnnotation(JsonComponent.class);
			Map<Class<?>, List<Class<?>>> innerComponents = new HashMap<>();
			for (String jsonComponent : jsonComponents) {
				Class<?> type = beanFactory.getType(jsonComponent, true);
				for (Class<?> declaredClass : type.getDeclaredClasses()) {
					if (isSuitableInnerClass(declaredClass)) {
						innerComponents.computeIfAbsent(type, (t) -> new ArrayList<>()).add(declaredClass);
					}
				}
			}
			return innerComponents.isEmpty() ? null : new JsonComponentAotContribution(innerComponents);
		}

	}

	/**
	 * JsonComponentAotContribution class.
	 */
	private static final class JsonComponentAotContribution implements BeanFactoryInitializationAotContribution {

		private final Map<Class<?>, List<Class<?>>> innerComponents;

		/**
		 * Constructs a new JsonComponentAotContribution object with the specified inner
		 * components.
		 * @param innerComponents a map containing the inner components, where the key is
		 * the outer component class and the value is a list of inner component classes
		 */
		private JsonComponentAotContribution(Map<Class<?>, List<Class<?>>> innerComponents) {
			this.innerComponents = innerComponents;
		}

		/**
		 * Applies the given generation context and bean factory initialization code to
		 * this JsonComponentAotContribution.
		 * @param generationContext The generation context to apply.
		 * @param beanFactoryInitializationCode The bean factory initialization code to
		 * apply.
		 */
		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
			this.innerComponents.forEach((outer, inners) -> {
				reflection.registerType(outer, MemberCategory.DECLARED_CLASSES);
				inners.forEach((inner) -> reflection.registerType(inner, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
			});
		}

	}

}
