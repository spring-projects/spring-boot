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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.Environment;
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
 * @author Ashish Vaghela
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public @Nullable ConfigurationPropertiesReflectionHintsContribution processAheadOfTime(
			ConfigurableListableBeanFactory beanFactory) {
		String[] beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
		List<Bindable<?>> bindables = new ArrayList<>();
		Map<String, Bindable<?>> bindablesByPrefix = new LinkedHashMap<>();
		for (String beanName : beanNames) {
			Class<?> beanType = beanFactory.getType(beanName, false);
			if (beanType != null) {
				BindMethod bindMethod = beanFactory.containsBeanDefinition(beanName)
						? (BindMethod) beanFactory.getBeanDefinition(beanName).getAttribute(BindMethod.class.getName())
						: null;
				Bindable<?> bindable = Bindable.of(ClassUtils.getUserClass(beanType))
					.withBindMethod((bindMethod != null) ? bindMethod : BindMethod.JAVA_BEAN);
				bindables.add(bindable);
				bindablesByPrefix.put(getPrefix(beanFactory, beanName), bindable);
			}
		}
		if (bindables.isEmpty()) {
			return null;
		}
		Environment environment = beanFactory.getBeanProvider(Environment.class).getIfAvailable();
		return new ConfigurationPropertiesReflectionHintsContribution(bindables, bindablesByPrefix, environment);
	}

	private String getPrefix(ConfigurableListableBeanFactory beanFactory, String beanName) {
		ConfigurationProperties annotation = beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class);
		return (annotation != null) ? annotation.prefix() : "";
	}

	static final class ConfigurationPropertiesReflectionHintsContribution
			implements BeanFactoryInitializationAotContribution {

		private static final Log logger = LogFactory.getLog(ConfigurationPropertiesReflectionHintsContribution.class);

		private final List<Bindable<?>> bindables;

		private final Map<String, Bindable<?>> bindablesByPrefix;

		private final @Nullable Environment environment;

		private ConfigurationPropertiesReflectionHintsContribution(List<Bindable<?>> bindables,
				Map<String, Bindable<?>> bindablesByPrefix, @Nullable Environment environment) {
			this.bindables = bindables;
			this.bindablesByPrefix = bindablesByPrefix;
			this.environment = environment;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			RuntimeHints hints = generationContext.getRuntimeHints();
			BindableRuntimeHintsRegistrar.forBindables(this.bindables).registerHints(hints);
			registerConfiguredClassHints(hints);
		}

		/**
		 * Register reflection hints for the values of any property of type {@link Class}.
		 * Such values are typically instantiated reflectively by the component to which
		 * they are given.
		 * @param hints the hints to contribute to
		 */
		private void registerConfiguredClassHints(RuntimeHints hints) {
			Set<Class<?>> classValues = new LinkedHashSet<>();
			this.bindablesByPrefix.values().forEach((bindable) -> classValues.addAll(getDefaultClassValues(bindable)));
			if (this.environment != null) {
				classValues.addAll(getConfiguredClassValues(this.environment));
			}
			for (Class<?> type : classValues) {
				hints.reflection().registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
		}

		private Set<Class<?>> getConfiguredClassValues(Environment environment) {
			Binder binder = Binder.get(environment);
			ClassValueBindHandler handler = new ClassValueBindHandler();
			this.bindablesByPrefix.forEach((prefix, bindable) -> {
				try {
					binder.bind(prefix, bindable, handler);
				}
				catch (Exception ex) {
					logger.debug("Skipping class value hints for '" + prefix + "'", ex);
				}
			});
			return handler.getClassValues();
		}

		private List<Class<?>> getDefaultClassValues(Bindable<?> bindable) {
			Class<?> type = bindable.getType().resolve();
			if (type == null) {
				return List.of();
			}
			try {
				Object instance = BeanUtils.instantiateClass(type);
				List<Class<?>> classValues = new ArrayList<>();
				ReflectionUtils.doWithFields(type, (field) -> {
					ReflectionUtils.makeAccessible(field);
					if (ReflectionUtils.getField(field, instance) instanceof Class<?> classValue) {
						classValues.add(classValue);
					}
				}, (field) -> field.getType() == Class.class && !Modifier.isStatic(field.getModifiers()));
				return classValues;
			}
			catch (Exception ex) {
				logger.debug("Skipping default class value hints for '" + type.getName() + "'", ex);
				return List.of();
			}
		}

		Iterable<Bindable<?>> getBindables() {
			return this.bindables;
		}

	}

	/**
	 * {@link BindHandler} that collects the values of any bound property of type
	 * {@link Class}.
	 */
	private static final class ClassValueBindHandler implements BindHandler {

		private final Set<Class<?>> classValues = new LinkedHashSet<>();

		@Override
		public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
				Object result) {
			if (result instanceof Class<?> classValue) {
				this.classValues.add(classValue);
			}
			return result;
		}

		Set<Class<?>> getClassValues() {
			return this.classValues;
		}

	}

}
