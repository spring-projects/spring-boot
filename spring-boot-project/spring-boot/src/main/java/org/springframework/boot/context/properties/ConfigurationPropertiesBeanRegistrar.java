/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Delegate used by {@link EnableConfigurationPropertiesRegistrar} and
 * {@link ConfigurationPropertiesScanRegistrar} to register a bean definition for a
 * {@link ConfigurationProperties @ConfigurationProperties} class.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Yanming Zhou
 */
final class ConfigurationPropertiesBeanRegistrar {

	private static final ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private final BeanDefinitionRegistry registry;

	private final BeanFactory beanFactory;

	ConfigurationPropertiesBeanRegistrar(BeanDefinitionRegistry registry) {
		this.registry = registry;
		this.beanFactory = (BeanFactory) this.registry;
	}

	void register(Class<?> type) {
		MergedAnnotation<ConfigurationProperties> annotation = MergedAnnotations
			.from(type, SearchStrategy.TYPE_HIERARCHY)
			.get(ConfigurationProperties.class);
		register(type, annotation);
	}

	void register(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
		String name = getName(type, annotation);
		if (!containsBeanDefinition(name)) {
			registerBeanDefinition(name, type, annotation);
		}
	}

	private String getName(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
		String prefix = annotation.isPresent() ? annotation.getString("prefix") : "";
		return (StringUtils.hasText(prefix) ? prefix + "-" + type.getName() : type.getName());
	}

	private boolean containsBeanDefinition(String name) {
		return (this.beanFactory instanceof ListableBeanFactory listableBeanFactory
				&& listableBeanFactory.containsBeanDefinition(name));
	}

	private void registerBeanDefinition(String beanName, Class<?> type,
			MergedAnnotation<ConfigurationProperties> annotation) {
		Assert.state(annotation.isPresent(), () -> "No " + ConfigurationProperties.class.getSimpleName()
				+ " annotation found on  '" + type.getName() + "'.");
		BeanDefinitionReaderUtils.registerBeanDefinition(createBeanDefinition(beanName, type), this.registry);
	}

	private BeanDefinitionHolder createBeanDefinition(String beanName, Class<?> type) {
		AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(type);
		AnnotationConfigUtils.processCommonDefinitionAnnotations(definition);
		BindMethod bindMethod = ConfigurationPropertiesBean.deduceBindMethod(type);
		BindMethodAttribute.set(definition, bindMethod);
		if (bindMethod == BindMethod.VALUE_OBJECT) {
			definition.setInstanceSupplier(() -> ConstructorBound.from(this.beanFactory, beanName, type));
		}
		ScopeMetadata metadata = scopeMetadataResolver.resolveScopeMetadata(definition);
		definition.setScope(metadata.getScopeName());
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definition, beanName);
		return applyScopedProxyMode(metadata, definitionHolder, this.registry);
	}

	static BeanDefinitionHolder applyScopedProxyMode(ScopeMetadata metadata, BeanDefinitionHolder definition,
			BeanDefinitionRegistry registry) {
		ScopedProxyMode mode = metadata.getScopedProxyMode();
		if (mode != ScopedProxyMode.NO) {
			return ScopedProxyUtils.createScopedProxy(definition, registry, mode == ScopedProxyMode.TARGET_CLASS);
		}
		return definition;
	}

}
