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

package org.springframework.boot.context.properties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} that contributes runtime hints for
 * configuration properties-annotated beans.
 *
 * @author Stephane Nicoll
 * @author Christoph Strobl
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public ConfigurationPropertiesReflectionHintsContribution processAheadOfTime(
			ConfigurableListableBeanFactory beanFactory) {
		String[] beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
		List<Bindable<?>> bindables = new ArrayList<>();
		Map<Class<?>, Object> instances = new HashMap<>();
		for (String beanName : beanNames) {
			Class<?> beanType = beanFactory.getType(beanName, false);
			if (beanType != null) {
				BindMethod bindMethod = beanFactory.containsBeanDefinition(beanName)
						? (BindMethod) beanFactory.getBeanDefinition(beanName).getAttribute(BindMethod.class.getName())
						: null;
				Class<?> userClass = ClassUtils.getUserClass(beanType);
				bindables.add(Bindable.of(userClass)
					.withBindMethod((bindMethod != null) ? bindMethod : BindMethod.JAVA_BEAN));
				try {
					instances.putIfAbsent(userClass, beanFactory.getBean(beanName));
				}
				catch (Exception ex) {
					// Ignore and continue without an instance
				}
			}
		}
		return (!bindables.isEmpty()) ? new ConfigurationPropertiesReflectionHintsContribution(bindables, instances)
				: null;
	}

	static final class ConfigurationPropertiesReflectionHintsContribution
			implements BeanFactoryInitializationAotContribution {

		private final List<Bindable<?>> bindables;

		private final Map<Class<?>, Object> instances;

		private ConfigurationPropertiesReflectionHintsContribution(List<Bindable<?>> bindables,
				Map<Class<?>, Object> instances) {
			this.bindables = bindables;
			this.instances = instances;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
			BindableRuntimeHintsRegistrar.forBindables(this.bindables)
				.registerHints(generationContext.getRuntimeHints());
			Set<Class<?>> seen = new HashSet<>();
			for (Map.Entry<Class<?>, Object> entry : this.instances.entrySet()) {
				registerClassPropertyHints(reflection, entry.getKey(), entry.getValue(), seen);
			}
		}

		private static void registerClassPropertyHints(ReflectionHints reflection, Class<?> type, Object instance,
				Set<Class<?>> seen) {
			if (instance == null || isJavaType(type) || !seen.add(type)) {
				return;
			}
			ReflectionUtils.doWithFields(type, (field) -> processField(reflection, field, instance, seen));
		}

		private static void processField(ReflectionHints reflection, Field field, Object instance, Set<Class<?>> seen) {
			if (Modifier.isStatic(field.getModifiers())) {
				return;
			}
			Class<?> fieldType = field.getType();
			if (fieldType.isPrimitive() || (isJavaType(fieldType) && fieldType != Class.class)) {
				return;
			}
			Object value;
			try {
				ReflectionUtils.makeAccessible(field);
				value = field.get(instance);
			}
			catch (Throwable ex) {
				return;
			}
			if (fieldType == Class.class) {
				if (value instanceof Class<?> classValue) {
					reflection.registerType(classValue, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
				}
				return;
			}
			if (value != null) {
				registerClassPropertyHints(reflection, fieldType, value, seen);
			}
		}

		private static boolean isJavaType(Class<?> type) {
			return type.getPackageName().startsWith("java.");
		}

		Iterable<Bindable<?>> getBindables() {
			return this.bindables;
		}

	}

}
